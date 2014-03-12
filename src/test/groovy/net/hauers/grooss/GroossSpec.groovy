package net.hauers.grooss

@Grab(group='org.spockframework', module='spock-core', version='0.7-groovy-2.0')
@Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version= '0.7.1')
@Grab(group='commons-lang', module= 'commons-lang', version= '2.4')
@Grab(group='org.apache.httpcomponents', module='httpmime', version='4.2.6') 

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

    @IgnoreRest
    def "We can upload an image"() {

        def api_secret = "24340403e58ce663c9119611626988a1"
        def default_secret = "d601c944a7a8a0c7c28633922e6509ccac996b0d43eac991d001acd69f070efd"

        def signature = hmac(new File('/tmp/test.jpg').text, 
            "${api_secret}&${default_secret}" ).toString().bytes.encodeBase64().toString()

//        auth_header = "${public_key}:${hmac(public_key, private_key)}".
//                    toString().bytes.encodeBase64().toString()
//con is a HttpURLConnection
//con.setRequestProperty('X-Mashape-Authorization', auth_header)
//        def http = new HTTPBuilder( "http://localhost:8000" ) 
        def http = new HTTPBuilder( "http://upload.smugmug.com/")
        def response = http.request(POST) { req ->
            uri.path = ''
            headers.'Authorization' = """OAuth,
            oauth_consumer_key=\"wvC8CH5jOwP6sPl8lxjnRpMi4krngGz4\",
            oauth_token=\"444f3de6c8aa46fe392aa19741610cd4\",
            oauth_signature_method=\"HMAC-SHA1\",
            oauth_signature=\"${signature}\",
            oauth_timestamp=\"${(int) System.currentTimeMillis() / 1000}\",
            oauth_nonce=\"${RandomStringUtils.randomAscii( 10 )}\",
            oauth_version=\"1.0\""""
            headers.'X-Smug-Version' = '1.3.0'
            headers.'X-Smug-AlbumID' = '4982718'
            req.entity = new FileEntity(new File('/tmp/test.jpg'),'image/jpg')
        }

        println XmlUtil.serialize(response)

        expect:
        true
    }


    def hmac(String data, String key) throws java.security.SignatureException
    {
        String result
        
        try {
        
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(key.getBytes(), "HmacSHA1");
            
            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);
            
            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(data.getBytes());
            result= rawHmac.encodeHex()
        
        } catch (Exception e) {
            throw new SignatureException("Failed to generate HMAC : " + e.getMessage());
        }
     
        return result
    }
}
/*
9.4.  PLAINTEXT

The PLAINTEXT method does not provide any security protection and SHOULD only be used over a secure channel such as HTTPS. It does not use the Signature Base String.

9.4.1.  Generating Signature

oauth_signature is set to the concatenated encoded values of the Consumer Secret and 
Token Secret, separated by a ‘&’ character (ASCII code 38), even if either secret is empty. 
The result MUST be encoded again.

These examples show the value of oauth_signature for Consumer Secret djr9rjt0jd78jf88 
and 3 different Token Secrets:

jjd999tj88uiths3:
oauth_signature=djr9rjt0jd78jf88%26jjd999tj88uiths3
jjd99$tj88uiths3:
oauth_signature=djr9rjt0jd78jf88%26jjd99%2524tj88uiths3
Empty:
oauth_signature=djr9rjt0jd78jf88%26


9.2.  HMAC-SHA1

The HMAC-SHA1 signature method uses the HMAC-SHA1 signature algorithm as defined in [RFC2104] 
where the Signature Base String is the text and the key is the concatenated values 
(each first encoded per Parameter Encoding) of the Consumer Secret and Token Secret, 
separated by an ‘&’ character (ASCII code 38) even if empty.

9.2.1.  Generating Signature

oauth_signature is set to the calculated digest octet string, 
first base64-encoded per [RFC2045] section 6.8, then URL-encoded per Parameter Encoding.

*/