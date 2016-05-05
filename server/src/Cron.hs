-- Cron which erase inactive tokens

module Cron where

import Control.Monad
import Data.Time.Clock
import Control.Concurrent
import Data.Text (Text)
import Database.Persist

import Api
import Types
import Constant
import Database

checkAndErase :: MVar MessagePool -> IO ()
checkAndErase msgp = do
    putStrLn "performing cron task to erase inactive tokens..."
    curr1 <- getCurrentTime
    tups <- getInactiveToken sqlTable
    res <- forM tups $ \(tok, uid) -> deleteTokenDb tok uid msgp
    let nAll = length tups
        nFinished = length $ filter id res
    curr2 <- getCurrentTime
    if and res then do
        putStrLn $ "all done, time elapsed: " ++ show (diffUTCTime curr2 curr1)
        putStrLn $ show nAll ++ " tokens cleaned"
    else do
        putStrLn $ show nFinished ++ "/" ++ show nAll ++
            " finished, this is very unlikely to happen!"
        putStrLn $ "time elapsed: " ++ show (diffUTCTime curr2 curr1)

getInactiveToken :: Text -> IO [(Token, Key User)]
getInactiveToken tableName = undefined

clearTokenCron :: MVar MessagePool -> IO ()
clearTokenCron msgp = forever $ do
    checkAndErase msgp
    threadDelay (3 * 24 * 3600 * 1000) -- 3 day
