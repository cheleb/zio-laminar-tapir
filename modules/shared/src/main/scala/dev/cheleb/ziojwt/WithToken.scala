package dev.cheleb.ziojwt

trait WithToken {
  def issuer: String
  def token: String
  val expiration: Long
}
