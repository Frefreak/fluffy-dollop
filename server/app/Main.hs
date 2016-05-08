{-# LANGUAGE OverloadedStrings #-}
module Main where

import Network.Wai.Handler.WebSockets as WaiWs
import Network.Wai.Handler.Warp as Warp
import Network.WebSockets
import Network.Wai
import Network.Wai.Middleware.RequestLogger
import Network.HTTP.Types.Status
import Database.Persist
import Database.Persist.Sql
import Database.Persist.TH
import Database.Persist.Sqlite
import Control.Concurrent
import Control.Monad
import Data.HashMap.Lazy as HM
import Data.Default

import Api
import Database
import Constant
import Types
import Logger
import Cron
import Application (cliperApp)

app :: ServerApp
app pend =
    case requestPath $ pendingRequest pend of
        "/register" -> do
            conn <- acceptRequest pend
            appRegister conn
        "/login" -> do
            conn <- acceptRequest pend
            appLogin conn
        "/post" -> do
            conn <- acceptRequest pend
            appPost conn
        "/logout" -> do
            conn <- acceptRequest pend
            appLogout conn
        "/sync" -> do
            conn <- acceptRequest pend
            appSync conn
        "/ping" -> do
            conn <- acceptRequest pend
            appPing conn
        _ -> logNoUrl pend >> rejectRequest pend ""

main :: IO ()
main = do
    runSqlite sqlTable (runMigration migrateAll)
    logger <- customLogger
    void $ forkIO clearTokenCron
    Warp.runSettings (Warp.setPort 4564 Warp.defaultSettings) $ logger
        $ WaiWs.websocketsOr defaultConnectionOptions
            app cliperApp

customLogger :: IO Middleware
customLogger = mkRequestLogger def {outputFormat = Apache FromSocket}

-- non-websockets connection, all return 404
defaultApp :: Application
defaultApp req respond = respond $ responseLBS status404 [] ""
