package dev.cheleb.ziojwt

trait WithToken {
  def token: String
  val expiration: Long
}
