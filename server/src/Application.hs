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
import Text.Blaze.Html.Renderer.Text (renderHtml)
import Text.Blaze.Html (toHtml)
import Control.Monad.Trans.Either

import qualified JSON as J
import Resource

type CliperApi =                    Get '[HTML] Html
            :<|> Raw
            {-:<|> "register" :> Post '[JSON] J.JRegister-}

cliperServer :: Server CliperApi
cliperServer = homeServer :<|> staticServer
        {-:<|>    registerPostServer-}

homeServer :: Handler Html
homeServer = return homeTemplate

staticServer :: Server Raw
staticServer = serveDirectory "static"

cliperAPI :: Proxy CliperApi
cliperAPI = Proxy

cliperApp :: Application
cliperApp = serve cliperAPI cliperServer

