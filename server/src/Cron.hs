-- Cron which erase inactive tokens
{-# LANGUAGE ScopedTypeVariables #-}

module Cron where

import Control.Monad
import Data.Time.Clock
import Control.Concurrent
import Data.Text (Text)
import Database.Persist
import Database.Persist.Sqlite
import Data.HashMap.Lazy (size)
import System.IO

import Api
import Types
import Constant
import Database

checkAndErase :: IO ()
checkAndErase = do
    hSetBuffering stdout NoBuffering
    putStrLn $ replicate 80 '='
    putStrLn "performing cron task to erase inactive tokens..."
    curr1 <- getCurrentTime
    tups <- getInactiveToken sqlTable inactiveTimeToDelete
    res <- forM tups $ \(tok, uid) -> deleteTokenDb tok uid
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
    act :: [Entity MessagePool] <- runSqlite sqlTable $ selectList [] []
    putStrLn $ "# of currently active tokens: " ++ show (length act)
    putStrLn $ replicate 80 '-'
    hSetBuffering stdout LineBuffering

-- inactiveTime's unit: second
getInactiveToken :: Text -> NominalDiffTime -> IO [(Token, Key User)]
getInactiveToken tableName inactiveTime = do
    cur <- getCurrentTime
    allTokens :: [Entity TokenMap] <- runSqlite sqlTable $ selectList [] []
    let inactive = filter (\(Entity tid tkm) ->
            diffUTCTime cur (tokenMapLastseen tkm) > inactiveTime) allTokens
    return $ map (\(Entity tid tkm) -> (tokenMapToken tkm, tokenMapUser tkm)) inactive

clearTokenCron :: IO ()
clearTokenCron = forever $ do
    checkAndErase
    threadDelay peroidToClean
