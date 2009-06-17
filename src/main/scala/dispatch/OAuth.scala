package dispatch.oauth
import Http.{%, q_str}

import collection.Map
import collection.immutable.TreeMap

import javax.crypto

import org.apache.http.protocol.HTTP.UTF_8
import org.apache.commons.codec.binary.Base64.encodeBase64

case class Consumer(key: String, secret: String)
case class Token(value: String, secret: String)

object OA { // scalac thinks this method is recurisve if it's in OAuth (?)
  implicit def add_<<@ (r: Request) = new {
    def <<@ (consumer: Consumer, token: Option[Token]) = r next {
      case before: Post =>
        val after = new Post(OAuth.sign(before.getMethod, before.getURI.toString, before.values, consumer, token))
        r.mimic(after)(before)
      case before => before
    }
  }
}

object OAuth {
  def sign(method: String, url: String, user_params: Map[String, Any], consumer: Consumer, token: Option[Token]) = {
    val params = TreeMap(
      "oauth_consumer_key" -> consumer.key,
      "oauth_signature_method" -> "HMAC-SHA1",
      "oauth_version" -> "1.0",
      "oauth_timestamp" -> (System.currentTimeMillis / 1000).toString,
      "oauth_nonce" -> System.nanoTime.toString
    ) ++ token.map { "oauth_token" -> _.value } ++ user_params
    
    val message = %%(method :: url :: q_str(params) :: Nil)
    println(message)
    
    val SHA1 = "HmacSHA1";
    val key_str = %%(consumer.secret :: (token map { _.secret } getOrElse "") :: Nil)
    val key = new crypto.spec.SecretKeySpec(key_str.getBytes(UTF_8), SHA1)
    val sig = {
      val mac = crypto.Mac.getInstance(SHA1)
      mac.init(key)
      new String(encodeBase64(mac.doFinal(bytes(message))))
    }
    params + ("oauth_signature" -> sig)
  }

  def %% (s: Seq[String]) = s map % mkString "&"
  def bytes(str: String) = str.getBytes(UTF_8)
}