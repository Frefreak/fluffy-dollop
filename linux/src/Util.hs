{-# LANGUAGE OverloadedStrings #-}
module Util where

import System.IO
import System.Process
import System.Random
import Control.Lens
import System.FilePath.Posix
import qualified Data.Text as T
import qualified Data.Text.IO as T
import Data.Monoid 
import Types
import Constant
import Control.Exception

askForValue :: (Show a, Read a) => String -> a -> IO a
askForValue prompt def = do
    putStrLn $ prompt ++ " (default: " ++ show def ++ "):"
    cand <- getLine
    return $ if null cand then def else read cand

askForStringValue :: String -> String -> IO String
askForStringValue prompt def = do
    putStrLn $ prompt ++ " (default: " ++ def ++ "):"
    cand <- getLine
    return $ if null cand then def else cand

askForPassword :: String -> IO String
askForPassword prompt = do
    putStrLn prompt
    hSetEcho stdin False
    pwd <- getLine
    hSetEcho stdin True
    return pwd

getDeviceName :: IO String
getDeviceName = init <$> readProcess "uname" ["-r"] []

genRandomSequence :: Int -> IO String
genRandomSequence n = do
    gen <- newStdGen
    return . take n $ randomRs ('a', 'z') gen

tokenToCache :: String -> Token -> T.Text
tokenToCache username token = T.pack username <> "\n" <> token

cacheToToken :: T.Text -> (String, Token)
cacheToToken text =
    let (u:t) = T.lines text
    in (T.unpack u, T.concat t)

getTokenFromConfig :: Configuration -> IO Token
getTokenFromConfig conf = do
    let storedir = conf ^. storeInfoL . storeDirL
        cacheFile = storedir </> cacheFileName
    res <- try (snd . cacheToToken <$> T.readFile cacheFile)
    case res of
        Left e -> do
            print (e :: IOException)
            error "Have you logged in?"
        Right r -> return r

getCacheFileFromConfig :: Configuration -> FilePath
getCacheFileFromConfig conf = conf ^. storeInfoL . storeDirL </> cacheFileName

getLogFileFromConfig :: Configuration -> FilePath
getLogFileFromConfig conf = 
    let u = conf ^. userInfoL . usernameL
    in conf ^. storeInfoL . storeDirL </> (u <> "_" <> logFileName)

setClipboard :: String -> IO ()
setClipboard msg = do
    (stdIn, _, _, _) <- 
        createProcess (shell "xclip -selection clipboard") {std_in = CreatePipe}
    case stdIn of
        Just i -> do
            hPutStr i msg
            hClose i
        Nothing -> putStrLn "fail to set clipboard content"