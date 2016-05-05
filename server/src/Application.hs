{-# LANGUAGE DataKinds, TypeOperators #-}
{-# LANGUAGE QuasiQuotes #-}
{-# LANGUAGE TemplateHaskell #-}
{-# LANGUAGE OverloadedStrings #-}

module Application where

import Data.Text
import Servant
import Servant.HTML.Blaze
import Text.Hamlet
import Text.Hamlet.Runtime
import Network.Wai
import qualified Data.Map as M
import Text.Blaze.Html.Renderer.Text (renderHtml)
import Text.Blaze.Html (toHtml)
import Control.Monad.Catch
import Control.Monad.IO.Class

import qualified JSON as J

devTemplate :: (MonadThrow m, MonadIO m) => FilePath -> m HamletTemplate
devTemplate = readHamletTemplateFile defaultHamletSettings

runtimeHtml :: (MonadThrow m, MonadIO m) => FilePath -> M.Map Text HamletData -> m Html
runtimeHtml fp m = devTemplate fp >>= flip renderHamletTemplate m

type CliperApi = "register" :> Get '[HTML] Html
            {-:<|> "register" :> Post '[JSON] J.JRegister-}

cliperServer :: Server CliperApi
cliperServer =  registerGetServer
        {-:<|>    registerPostServer-}

registerGetServer = runtimeHtml "templates/register.hamlet" $
    M.singleton "username" "adv_zxy"

{-registerPostServer = undefined-}

cliperAPI :: Proxy CliperApi
cliperAPI = Proxy

cliperApp :: Application
cliperApp = serve cliperAPI cliperServer

