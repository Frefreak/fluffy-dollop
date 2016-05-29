{-# LANGUAGE OverloadedStrings #-}
module Constant where

import Data.Text (Text)
import Data.Time.Clock

sqlTable :: Text
sqlTable = "data.db"

sqlErrorStr :: Text
sqlErrorStr = "error while executing sql command"

inactiveTimeToDelete :: NominalDiffTime
inactiveTimeToDelete = 24 * 3600

peroidToClean :: Int
peroidToClean = 24 * 3600 * 1000000 -- 24 * 3600 * 1000 -- 1 day
