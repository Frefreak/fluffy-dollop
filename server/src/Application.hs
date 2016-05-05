{-# LANGUAGE DataKinds, TypeOperators #-}
{-# LANGUAGE QuasiQuotes #-}
{-# LANGUAGE TemplateHaskell #-}
{-# LANGUAGE OverloadedStrings #-}

module Application where

import Control.Monad.Trans.Either
import Data.Text
import Servant
import Servant.HTML.Blaze
import Text.Hamlet
import Text.Hamlet.Runtime
import Network.Wai
import qualified Data.Map as M
import Text.Blaze.Html.Renderer.Text (renderHtml)
import Text.Blaze.Html (toHtml)
import Control.Monad.Catch (MonadThrow)
import Control.Monad.IO.Class

import qualified JSON as J

devTemplate :: (MonadThrow m, MonadIO m) => FilePath -> m HamletTemplate
devTemplate = readHamletTemplateFile defaultHamletSettings

runtimeHtml :: (MonadThrow m, MonadIO m) => FilePath -> M.Map Text HamletData -> m Html
runtimeHtml fp m = devTemplate fp >>= flip renderHamletTemplate m

type CliperApi =                    Get '[HTML] Html
                :<|> "register" :> Get '[HTML] Html
            {-:<|> "register" :> Post '[JSON] J.JRegister-}

type Handler = EitherT ServantErr IO

cliperServer :: Server CliperApi
cliperServer = homeServer :<|> registerGetServer
        {-:<|>    registerPostServer-}

homeServer :: EitherT ServantErr IO Html
homeServer = runtimeHtml "templates/home.hamlet" mempty

registerGetServer :: Handler Html
registerGetServer = runtimeHtml "templates/register.hamlet" mempty

{-registerPostServer = undefined-}

cliperAPI :: Proxy CliperApi
cliperAPI = Proxy

cliperApp :: Application
cliperApp = serve cliperAPI cliperServer

