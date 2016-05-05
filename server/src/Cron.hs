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

import Api
import Types
import Constant
import Database

checkAndErase :: MVar MessagePool -> IO ()
checkAndErase msgp = do
    putStrLn "performing cron task to erase inactive tokens..."
    curr1 <- getCurrentTime
    tups <- getInactiveToken sqlTable inactiveTimeToDelete
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
    act <- size <$> readMVar msgp
    putStrLn $ "# of currently active tokens: " ++ show act

-- inactiveTime's unit: second
getInactiveToken :: Text -> NominalDiffTime -> IO [(Token, Key User)]
getInactiveToken tableName inactiveTime = do
    cur <- getCurrentTime
    allTokens :: [Entity TokenMap] <- runSqlite sqlTable $ selectList [] []
    let inactive = filter (\(Entity tid tkm) ->
            diffUTCTime cur (tokenMapLastseen tkm) > inactiveTime) allTokens
    return $ map (\(Entity tid tkm) -> (tokenMapToken tkm, tokenMapUser tkm)) inactive

test = do
    (allTokens :: [Entity TokenMap]) <- runSqlite sqlTable $ selectList [] []
    print allTokens

clearTokenCron :: MVar MessagePool -> IO ()
clearTokenCron msgp = forever $ do
    checkAndErase msgp
    threadDelay peroidToClean
