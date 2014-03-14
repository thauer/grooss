package net.hauers.grooss

@Grab(group='org.spockframework', module='spock-core', version='0.7-groovy-2.0')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version= '0.7.1')
@Grab(group='commons-lang', module= 'commons-lang', version= '2.4')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.2.6') 
@Grab( 'log4j:log4j:1.2.16' )

import spock.lang.*

import groovyx.net.http.HTTPBuilder
import org.apache.http.entity.mime.MultipartEntity
import org.apache.http.entity.FileEntity
import org.apache.http.entity.mime.content.ByteArrayBody
import static groovyx.net.http.Method.POST
import static groovyx.net.http.ContentType.JSON

import groovy.util.slurpersupport.NodeChild
import groovy.xml.*
import org.apache.commons.lang.RandomStringUtils

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import java.security.SignatureException
 
import java.net.URLEncoder

import groovy.util.logging.*

@Log4j
class GroossSpec extends Specification {

    Grooss grooss
    
    def setup() {
        grooss = new Grooss()
    }

    def defaultAuthorizationShouldWork() {
        
        expect: "checkAccessToken verifies that authentication is properly configured"
        grooss.oauthToken.id
        "ok" == grooss.checkAccessToken().stat
    }
    
    def getImagesShouldFetchProperNumberOfImageRecords() {
        expect:
        4 == grooss.getImages( AlbumID: '4982718', AlbumKey: 'VL2Sdx' ).size()
    }
    
    def imageInfoShouldBeReceivedOnExistingImage() {
        expect:
        'JPG' == grooss.getImageInfo( ImageID: '298563241', ImageKey: 'JQeBJ').Format
    }

    def getImageInfoShouldProvideDownloadableURL() {
        
        when:
        def image = grooss.getImageInfo(ImageID: '298563241', ImageKey: 'JQeBJ')
        def file  = grooss.downloadOriginal(image)
        def md5sum = java.security.MessageDigest.getInstance("MD5").with{
            file.withInputStream(){ is -> is.eachByte(8192) { 
                buffer, bytesRead -> it.update(buffer, 0, bytesRead) }}
            new BigInteger(1, it.digest()).toString(16).padLeft(32, '0')
        }
        then:
        md5sum == image.MD5Sum
        cleanup:
        file.delete()
    }

    /*
    Smugmug
    =======

    Raw HTTP Post:
    This method requires a POST request with the binary data in the body and all other metadata 
    in the headers.
    Endpoint: http://upload.smugmug.com/
    OAuth Headers:
    Authorization string (required)
        The OAuth authorization header as defined in the OAuth spec.
    Content-Length integer (recommended)
    Content-MD5 string (recommended)
    X-Smug-AlbumID integer (required)
    X-Smug-Pretty boolean
    X-Smug-Version string (required)


    RFC 5849 The OAuth 1.0 Protocol
    ===============================

    The signature base string includes the following components of the
    HTTP request:

    o  The HTTP request method (e.g., "GET", "POST", etc.).
    o  The authority as declared by the HTTP "Host" request header field.
    o  The path and query components of the request resource URI.
    o  The protocol parameters excluding the "oauth_signature".

    3.4.1.1.  String Construction

    The signature base string is constructed by concatenating together,
    in order, the following HTTP request elements:

    1.  The HTTP request method in uppercase.  For example: "POST"
    2.  An "&" character (ASCII code 38).
    3.  The base string URI from Section 3.4.1.2, after being encoded
    4.  An "&" character (ASCII code 38).
    5.  The request parameters as normalized in Section 3.4.1.3.2, after
        being encoded (Section 3.6).
    For example, the HTTP request:

        POST /request?b5=%3D%253D&a3=a&c%40=&a2=r%20b HTTP/1.1
        Host: example.com
        Content-Type: application/x-www-form-urlencoded
        Authorization: OAuth realm="Example",
                    oauth_consumer_key="9djdj82h48djs9d2",
                    oauth_token="kkk9d7dh3k39sjv7",
                    oauth_signature_method="HMAC-SHA1",
                    oauth_timestamp="137131201",
                    oauth_nonce="7d8f3e4a",
                    oauth_signature="bYT5CMsGcbgUdFHObYMEfcx6bsw%3D"

        c2&a3=2+q

    is represented by the following signature base string (whitespace is
    for display purposes only):

        POST & http%3A%2F%2Fexample.com%2Frequest & a2%3Dr%2520b%26 a3%3D2%2520q%26 
        a3%3Da%26 b5%3D%253D%25253D%26 c%2540%3D%26 c2%3D%26
        oauth_consumer_key%3D9djdj82h48djs9d2%26
        oauth_nonce%3D7d8f3e4a%26
        oauth_signature_method%3DHMAC-SHA1%26
        oauth_timestamp%3D137131201%26
        oauth_token%3Dkkk9d7dh3k39sjv7

    3.4.1.2.  Base String URI

    The scheme, authority, and path of the request resource URI [RFC3986] are included by 
    constructing an "http" or "https" URI representing the request resource (without the query 
    or fragment) as follows:

    1.  The scheme and host MUST be in lowercase.

    2.  The host and port values MUST match the content of the HTTP request "Host" header field.

    3.  The port MUST be included if it is not the default port for the scheme,
        and MUST be excluded if it is the default.

        The HTTP request:               | is represented by:
        -----------------------------------------------------------------
        GET /r%20v/X?id=123 HTTP/1.1    | http://example.com/r%20v/X
        Host: EXAMPLE.COM:80            |
                                        |
        GET /?q=1 HTTP/1.1              | https://www.example.net:8080/
        Host: www.example.net:8080      |


    3.4.2.  HMAC-SHA1

    The "HMAC-SHA1" signature method uses the HMAC-SHA1 algorithm as defined in [RFC2104]:

    digest = HMAC-SHA1 (key, text)

    The HMAC-SHA1 function variables are used in following way:

    text        is set to the value of the signature base string from Section 3.4.1.1.

    key         is set to the concatenated values of:

    1.  The client shared-secret, after being encoded (Section 3.6).
    2.  An "&" character, which MUST be included even when either secret is empty.
    3.  The token shared-secret, after being encoded (Section 3.6).

    digest  is used to set the value of the "oauth_signature" protocol parameter, after the 
    result octet string is base64-encoded per [RFC2045], Section 6.8.

    3.4.4.  PLAINTEXT

    The "PLAINTEXT" method does not employ a signature algorithm.  It MUST be used with a 
    transport-layer mechanism such as TLS or SSL (or sent over a secure channel with equivalent 
    protections).  It does not utilize the signature base string or the "oauth_timestamp" 
    and "oauth_nonce" parameters.

    The "oauth_signature" protocol parameter is set to the concatenated value of:

    1.  The client shared-secret, after being encoded (Section 3.6).
    2.  An "&" character, which MUST be included even when either secret is empty.
    3.  The token shared-secret, after being encoded (Section 3.6).
    */

    @IgnoreRest
    def "Authentication with oauth"() {
        log.info("STARTING")

        def token_api_id = "wvC8CH5jOwP6sPl8lxjnRpMi4krngGz4"
        def token_api_secret = "24340403e58ce663c9119611626988a1"
        def token_default_id = "444f3de6c8aa46fe392aa19741610cd4"
        def token_default_secret = "d601c944a7a8a0c7c28633922e6509ccac996b0d43eac991d001acd69f070efd"
        String nonce = RandomStringUtils.randomAscii(10)
        String timestamp = (System.currentTimeMillis() / 1000) as String

        String baseStringURI = "http://upload.smugmug.com/"
        String quoted_params = URLEncoder.encode(
            "oauth_consumer_key=${token_api_id}&" +
            "oauth_nonce=${nonce}&" +
            "oauth_signature_method=HMAC-SHA1&" +
            "oauth_timestamp=${timestamp}&" +
            "oauth_token=${token_default_id}&" +
            "oauth_version=1.0")

        String signatureBase = "POST&${URLEncoder.encode(baseStringURI)}&${quoted_params}"

        def key = "${token_api_secret}&${token_default_secret}"
        def rawMac = Mac.getInstance("HmacSHA1").with{
            init(new SecretKeySpec(key.bytes, "HmacSHA1"))
            doFinal(signatureBase.bytes)
        }
        def signature = rawMac.encodeBase64().toString()

        def http = new HTTPBuilder( "http://upload.smugmug.com/")
        def response = http.request(POST) { req ->
            uri.path = ''
            headers.'Authorization' = """OAuth,
            oauth_consumer_key=\"${token_api_id}\",
            oauth_nonce=\"${nonce}\",
            oauth_signature_method=\"HMAC-SHA1\",
            oauth_timestamp=\"${timestamp}\",
            oauth_token=\"${token_default_id}\",
            oauth_version=\"1.0\",
            oauth_signature=\"${signature}\""""
            req.entity = new FileEntity(new File('/tmp/test.jpg'),'image/jpg')
        }

        println XmlUtil.serialize(response)

        expect:
        true
    }
}
