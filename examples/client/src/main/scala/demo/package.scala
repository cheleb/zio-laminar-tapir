package demo

import dev.cheleb.ziotapir.*

import zio.json.*
case class UserToken(token: String, expiration: Long, zozo: String)
    extends dev.cheleb.ziojwt.WithToken derives JsonCodec

given session: Session[UserToken] = SessionLive[UserToken]
