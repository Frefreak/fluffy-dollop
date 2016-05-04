{-# LANGUAGE DataKinds, TypeOperators #-}
{-# LANGUAGE QuasiQuotes #-}
{-# LANGUAGE TemplateHaskell #-}

module Application where

import Data.Text
import Servant
import Servant.HTML.Blaze
import Text.Hamlet
import Network.Wai
import Text.Blaze.Html.Renderer.Text (renderHtml)
import Text.Blaze.Html (toHtml)

import qualified JSON as J

type CliperApi = "register" :> Get '[HTML] Html
            {-:<|> "register" :> Post '[JSON] J.JRegister-}

cliperServer :: Server CliperApi
cliperServer =  registerGetServer
        {-:<|>    registerPostServer-}

registerGetServer = return . toHtml $ $(shamletFile "templates/register.hamlet")

registerPostServer = undefined

cliperAPI :: Proxy CliperApi
cliperAPI = Proxy

cliperApp :: Application
cliperApp = serve cliperAPI cliperServer

