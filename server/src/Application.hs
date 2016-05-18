{-# LANGUAGE DataKinds, TypeOperators #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE ScopedTypeVariables #-}

module Application where

import Data.Text as T
import Servant
import Servant.HTML.Blaze
import Text.Hamlet
import Text.Hamlet.Runtime
import Network.Wai
import Text.Blaze.Html.Renderer.Text (renderHtml)
import Text.Blaze.Html (toHtml)
import Control.Monad.Trans.Either
import Data.ByteString (ByteString)
import Database.Persist
import Database.Persist.Sqlite
import Control.Monad.IO.Class
import Data.Text.Encoding (encodeUtf8)
import Control.Exception hiding (Handler)
import Data.Aeson

import qualified JSON as J
import Resource
import Database
import Constant
import Crypto
import Api
import Types

type CliperApi = Get '[HTML] Html
        :<|> "register" :> ReqBody '[JSON] J.JWebAuth :> Post '[JSON] Value
        :<|> "login" :> ReqBody '[JSON] J.JWebAuth :> Post '[JSON] Value
        :<|> "tokens" :> Capture "token" T.Text :> Get '[HTML] Html
        :<|> "post" :> ReqBody '[JSON] J.JPost :> Post '[JSON] Value
        :<|> Raw

cliperServer :: Server CliperApi
cliperServer = homeServer
        :<|> registerPostServer
        :<|> loginPostServer
        :<|> tokenServer
        :<|> postPostServer
        :<|> staticServer

homeServer ::Handler Html
homeServer = return homeTemplate

loginPostServer :: J.JWebAuth -> Handler Value
loginPostServer jwa = do
    maybeUser <- runSqlite sqlTable $ selectFirst
        [UserUsername ==. J.jwausername jwa, UserPassword ==. J.jwapassword jwa] []
    case maybeUser of
        Just (Entity uid _) -> do
            tok <- liftIO genRandomToken
            b <- liftIO $ insertTokenDb tok uid
            if b then respondJWA tok ""
                else respondJWA "" "Something went wrong!"
        Nothing -> respondJWA "" "Username/Password mismatch!"

registerPostServer :: J.JWebAuth -> Handler Value
registerPostServer jwa = do
    let u = J.jwausername jwa
        p = J.jwapassword jwa
    if T.null u || T.null p then
        respondJWA "" "Username/Password cannot be empty!"
    else if T.length u < 3 || T.length p < 3 then
        respondJWA "" "Username/Password must be at least 3 characters long!"
    else do
        uid' <- liftIO $ try (runSqlite sqlTable $ insert $ User u p (generateAesKey u p))
        case uid' of
            Left (e :: SomeException) -> respondJWA "" "Username already taken"
            Right uid -> do
                tok <- liftIO genRandomToken
                b <- liftIO $ insertTokenDb tok uid
                if b then respondJWA tok ""
                    else respondJWA "" "Something went wrong!"

respondJWA :: Text -> Text -> Handler Value
respondJWA tok msg = return $
    object ["token" .= String tok, "message" .= String msg]

tokenServer :: T.Text -> Handler Html
tokenServer token = do
    r <- liftIO $ runSqlite sqlTable $ selectFirst [TokenMapToken ==. token] []
    case r of
        Nothing -> left err404
        Just (Entity _ _) -> return tokensTemplate

postPostServer :: J.JPost -> Handler Value
postPostServer jp = do
    let genPostRespMsg code msg = object ["code" .= Number code, "msg" .= String msg]
        token = J.jptoken jp
    r <- liftIO $ runSqlite sqlTable $ selectFirst [TokenMapToken ==. token] []
    case r of
        Nothing -> return $ genPostRespMsg 422 "No such Token"
        Just (Entity _ tokenmap) -> do
            liftIO $ addMessageToToken (tokenMapUser tokenmap) token (J.jpdata jp)
            return $ genPostRespMsg 200 ""

staticServer :: Server Raw
staticServer = serveDirectory "static"

cliperAPI :: Proxy CliperApi
cliperAPI = Proxy

cliperApp :: Application
cliperApp = serve cliperAPI cliperServer

