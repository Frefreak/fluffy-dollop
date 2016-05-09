{-# LANGUAGE DataKinds, TypeOperators #-}
{-# LANGUAGE OverloadedStrings #-}

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

import qualified JSON as J
import Resource
import Database
import Constant
import Crypto
import Api

type CliperApi = Get '[HTML] Html
        :<|> "register" :> ReqBody '[FormUrlEncoded] J.JWebAuth :> Post '[HTML] Html
        :<|> "login" :> ReqBody '[FormUrlEncoded] J.JWebAuth :> Post '[HTML] Html
        :<|> Raw

redirect303WithToken :: Monad m => ByteString -> ByteString -> EitherT ServantErr m a
redirect303WithToken url token = left err303 { errHeaders = [("Location", url), ("Set-Cookie", mappend "token=" token)] }

cliperServer :: Server CliperApi
cliperServer = homeServer
        :<|> registerPostServer
        :<|> loginPostServer
        :<|> staticServer

homeServer :: Handler Html
homeServer = return $ homeTemplate (Nothing :: Maybe J.JWebAuth)

loginPostServer :: J.JWebAuth -> Handler Html
loginPostServer jwa = do
    maybeUser <- runSqlite sqlTable $ selectFirst
        [UserUsername ==. J.jwausername jwa, UserPassword ==. J.jwapassword jwa] []
    case maybeUser of
        Just (Entity uid _) -> do
            tok <- liftIO genRandomToken
            b <- liftIO $ insertTokenDb tok uid
            if b then redirect303WithToken "/" (encodeUtf8 tok)
                else redirect303WithToken "/" ""

registerPostServer :: J.JWebAuth -> Handler Html
registerPostServer jwa = do
    let u = J.jwausername jwa
        p = J.jwapassword jwa
    if T.null u || T.null p then
        redirect303WithToken "/" ""
    else do
        uid <- runSqlite sqlTable $ insert $ User u p (generateAesKey u p)
        tok <- liftIO genRandomToken
        b <- liftIO $ insertTokenDb tok uid
        if b then redirect303WithToken "/" (encodeUtf8 tok)
            else redirect303WithToken "/" ""

staticServer :: Server Raw
staticServer = serveDirectory "static"

cliperAPI :: Proxy CliperApi
cliperAPI = Proxy

cliperApp :: Application
cliperApp = serve cliperAPI cliperServer

