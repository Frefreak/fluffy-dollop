{-# LANGUAGE QuasiQuotes #-}
{-# LANGUAGE TemplateHaskell #-}
module Resource where

import Text.Hamlet
import Text.Hamlet.Runtime
import Text.Blaze.Html.Renderer.Text (renderHtml)
import Text.Blaze.Html (toHtml)
import Control.Monad.Catch (MonadThrow)
import qualified Data.Map as M
import Control.Monad.IO.Class
import Control.Monad.Trans.Either
import Servant

type Handler = EitherT ServantErr IO

homeTemplate = $(shamletFile "templates/home.hamlet")
homeRTemplate = $(shamletFile "templates/homeR.hamlet")
headTemplate = $(shamletFile "templates/head.hamlet")
navTemplate = $(shamletFile "templates/nav.hamlet")
modalTemplate = $(shamletFile "templates/modal.hamlet")
footerTemplate = $(shamletFile "templates/footer.hamlet")


