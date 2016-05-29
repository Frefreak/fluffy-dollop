{-# LANGUAGE OverloadedStrings #-}
module Api where

import Control.Monad
import Network.WebSockets
import Data.Aeson
import Data.Text as T (Text, null, length)
import qualified Data.ByteString.Lazy.Char8 as LBS
import Database.Persist
import Database.Persist.Sqlite hiding (Connection)
import Data.Scientific
import Control.Exception
import qualified Data.List as L
import Control.Concurrent
import qualified Data.HashMap.Lazy as HM
import Control.Monad.IO.Class (liftIO)
import System.Timeout
import Data.Maybe (fromJust)
import Data.Time.Clock

import Crypto
import JSON
import Database
import Constant
import Types
import Logger

respondRegisterMessage :: Connection -> Scientific -> Text -> IO ()
respondRegisterMessage conn code msg = sendTextData conn (encode obj) where
        obj = object [ "code" .= Number code, "msg" .= String msg]

respondLoginMessage :: Connection -> Scientific -> Text -> Token -> IO ()
respondLoginMessage conn code msg token = sendTextData conn (encode obj) where
        obj = object [  "code" .= Number code,
                        "msg" .= String msg,
                        "token" .= String token]

respondLogoutMessage :: Connection -> Scientific -> Text -> IO ()
respondLogoutMessage = respondRegisterMessage

respondPostMessage :: Connection -> Scientific -> Text -> IO ()
respondPostMessage = respondRegisterMessage

respondSyncErrorMessage :: Connection -> Scientific -> Text -> IO ()
respondSyncErrorMessage = respondRegisterMessage

respondSyncMessage :: Connection -> Scientific -> Text -> IO ()
respondSyncMessage = respondSyncErrorMessage

respondPingMessage :: Connection -> Scientific -> Text -> IO ()
respondPingMessage = respondSyncMessage

appRegister :: Connection -> IO ()
appRegister conn = do
    d <- receiveData conn
    let jregister = decode d
    case jregister of
        Just jr -> performRegisterAction conn jr
        Nothing -> logInvalidJson d >>
                respondRegisterMessage conn 400 "bad request"

performRegisterAction :: Connection -> JRegister -> IO ()
performRegisterAction conn jr =
    catch (
        if T.null u || T.null p then
            respondRegisterMessage conn 422 "username/password cannot be empty"
        else if T.length u < 3 || T.length p < 3 then
            respondRegisterMessage conn 422 "username/password must be at least 3 characters long"
        else do
            runSqlite sqlTable $ insert $ User u p key
            respondRegisterMessage conn 200 "") handler where
                u = jrusername jr
                p = jrpassword jr
                key = generateAesKey u p
                handler (SomeException _) =
                    respondRegisterMessage conn 422
                        "exception during database operation, probably because the\
                        \ username has been taken"

appLogin :: Connection -> IO ()
appLogin conn = do
    d <- receiveData conn
    let jlogin = decode d
    case jlogin of
        Just jl -> performLoginAction conn jl
        Nothing -> logInvalidJson d >>
                respondLoginMessage conn 400 "bad request" ""

performLoginAction :: Connection -> JLogin -> IO ()
performLoginAction conn jl = do
    maybeUser <- runSqlite sqlTable $ selectFirst
        [UserUsername ==. jlusername jl, UserPassword ==. jlpassword jl] []
    case maybeUser of
        Just (Entity uid _) -> do
            tok <- genRandomToken
            b <- insertTokenDb tok uid
            if b then respondLoginMessage conn 200 "" tok else
                        respondLoginMessage conn 422 sqlErrorStr ""
        Nothing -> respondLoginMessage conn 422 "username / password mismatch" ""

insertTokenDb :: Token -> Key User -> IO Bool
insertTokenDb tok uid = do
    cur <- getCurrentTime
    catch (
        runSqlite sqlTable $ do
            insert $ TokenMap tok uid cur
            ts <- selectFirst [IdMapUser ==. uid] []
            case ts of
                Nothing ->
                    void $ insert $ IdMap uid [tok]
                Just (Entity rid (IdMap _ tks)) -> update rid [IdMapTokens =. (tok:tks)]
            insert $ MessagePool tok []
            return True) handler where
                handler (SomeException _) = return False

appPost :: Connection -> IO ()
appPost conn = do
    d <- receiveData conn
    let jpost = decode d
    case jpost of
        Just jp -> performPostAction conn jp
        Nothing -> logInvalidJson d >>
            respondPostMessage conn 400 "bad request"

performPostAction :: Connection -> JPost -> IO ()
performPostAction conn jp = do
    mtokenMap <- runSqlite sqlTable $ selectFirst [TokenMapToken ==. jptoken jp] []
    case mtokenMap of
        Just (Entity _ tokenmap) -> do
            respondPostMessage conn 200 ""
            updateDatabaseTimestamp $ jptoken jp
            addMessageToToken (tokenMapUser tokenmap) (jptoken jp) (jpdata jp)
        Nothing -> respondPostMessage conn 422 "no such token"

addMessageToToken :: Key User -> Token -> Text -> IO ()
addMessageToToken uid token msg = do
    midmap <- runSqlite sqlTable $ selectFirst [IdMapUser ==. uid] []
    case midmap of
        Nothing -> error "OMG!!! BUGS!" -- this should not happen
        Just (Entity _ (IdMap _ toks)) ->
            forM_ (L.delete token toks) (\tok -> runSqlite sqlTable $ do
                msgs <- selectFirst [MessagePoolToken ==. tok] []
                case msgs of
                    Nothing -> error "BUGS AGAIN!"
                    Just (Entity _ msgs') -> updateWhere [MessagePoolToken ==. tok]
                        [MessagePoolMessage =. messagePoolMessage msgs' ++ [msg]])

appLogout :: Connection -> IO ()
appLogout conn = do
    d <- receiveData conn
    let jlogout = decode d
    case jlogout of
        Just jl -> performLogoutAction conn jl
        Nothing -> logInvalidJson d >>
            respondLogoutMessage conn 400 "bad request"

performLogoutAction :: Connection -> JLogout -> IO ()
performLogoutAction conn jl = do
    mtokenMap <- runSqlite sqlTable $ selectFirst [TokenMapToken ==. jltoken jl] []
    case mtokenMap of
        Just (Entity _ tokenmap) -> do
            b <- deleteTokenDb (jltoken jl) (tokenMapUser tokenmap)
            if b then respondLogoutMessage conn 200 "" else
                    respondLogoutMessage conn 422 sqlErrorStr
        Nothing -> respondLogoutMessage conn 422 "no such token"

deleteTokenDb :: Token -> Key User -> IO Bool
deleteTokenDb tok uid =
    catch (runSqlite sqlTable $ do
        deleteWhere [TokenMapToken ==. tok]
        deleteWhere [MessagePoolToken ==. tok]
        ts <- selectFirst [IdMapUser ==. uid] []
        case ts of
            Nothing -> return False -- if this happens, we have severe bug(s)
            Just (Entity rid (IdMap _ tks)) -> do
                            update rid [IdMapTokens =. L.delete tok tks]
                            return True) handler where
                        handler (SomeException _) = return False

appSync :: Connection -> IO ()
appSync conn = do
    d <- receiveData conn
    let jsync = decode d
    case jsync of
        Nothing -> logInvalidJson d >>
            respondSyncErrorMessage conn 400 "bad request"
        Just js -> do
            maybeToken <- runSqlite sqlTable $ selectFirst [TokenMapToken ==. jstoken js] []
            case maybeToken of
                Just _ -> do
                    respondSyncMessage conn 200 ""
                    forkPingThread conn 30
                    updateDatabaseTimestamp (jstoken js)
                    sendMessagesOfToken (jstoken js) conn
                Nothing -> respondSyncErrorMessage conn 422 "no such token"

appPing :: Connection -> IO ()
appPing conn = do
    d <- receiveData conn
    let jping = decode d
    case jping of
        Nothing -> logInvalidJson d >>
            respondPingMessage conn 400 "bad request"
        Just jp -> do
            maybeToken <- runSqlite sqlTable $ selectFirst [TokenMapToken ==. jpingtoken jp] []
            case maybeToken of
                Just _ -> do
                    respondPingMessage conn 200 "ack"
                    updateDatabaseTimestamp $ jpingtoken jp
                Nothing ->
                    respondPingMessage conn 422 "no such token"

updateDatabaseTimestamp :: Token -> IO ()
updateDatabaseTimestamp token = do
    cur <- getCurrentTime
    runSqlite sqlTable $ updateWhere [TokenMapToken ==. token] [TokenMapLastseen =. cur]

sendMessagesOfToken :: Token -> Connection -> IO ()
sendMessagesOfToken token conn = do
    {-let handler (PatternMatchFail _) = return Nothing-}
    msgs <- runSqlite sqlTable $ selectFirst [MessagePoolToken ==. token] []
    case msgs of
        Nothing -> return () -- very unlikely to happen
        Just (Entity mid msgp) -> do
            let msgq = messagePoolMessage msgp
            if L.null msgq then do
                threadDelay 1000000
                sendMessagesOfToken token conn
            else do
                msgid <- genRandomMsgId
                let obj = object [ "msg" .= String (head msgq), "msgid" .= String msgid ]
                sendTextData conn (encode obj)
                d <- timeout 3000000 $ receiveData conn
                let jsa' = decode <$> d
                case jsa' of
                    Just (Just jsa) ->
                        if jsamsgid jsa == msgid && jsastatus jsa == "ok" then do
                            runSqlite sqlTable $ update mid [MessagePoolMessage =. tail msgq]
                            updateDatabaseTimestamp token
                            sendMessagesOfToken token conn else
                                sendMessagesOfToken token conn
                    Just Nothing -> logInvalidJson (fromJust d) >>
                                    respondSyncErrorMessage conn 400
                                    "you don't know how to respond to my message!"
                    Nothing -> respondSyncErrorMessage conn 400
                                    "you don't even know you should respond to my message!"

