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

import JSON
import Types

{-type Handler = EitherT ServantErr IO-}

homeTemplate :: Html
homeTemplate = $(shamletFile "templates/home.hamlet")

tokensTemplate :: Html
tokensTemplate = $(shamletFile "templates/tokens.hamlet")

homeRTemplate = $(shamletFile "templates/homeR.hamlet")
tokensRTemplate = $(shamletFile "templates/tokensR.hamlet")
headTemplate = $(shamletFile "templates/head.hamlet")
navTemplate = $(shamletFile "templates/nav.hamlet")
modalTemplate = $(shamletFile "templates/modal.hamlet")
footerTemplate = $(shamletFile "templates/footer.hamlet")
tailTemplate = $(shamletFile "templates/tail.hamlet")

