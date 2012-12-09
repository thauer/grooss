package net.hauers.grooss

import org.junit.Test
import org.junit.Ignore
import org.junit.BeforeClass

class GroossTest {

	static ConfigObject config = null
	
	@BeforeClass
	static void initializeConfig() {

		try {
			Class scriptClass = GroossTest.class.classLoader.loadClass( 'test-config' )
			config = new ConfigSlurper().parse(scriptClass)
		} catch( Exception e ){ e.printStackTrace() }
		assert config		
		
		File.metaClass.getMd5 = { ->
			def digest = java.security.MessageDigest.getInstance("MD5")
			delegate.withInputStream(){ is ->
				is.eachByte( 8192 ) { buffer, bytesRead ->
					digest.update( buffer, 0, bytesRead )
				}
			}
			new BigInteger( 1, digest.digest() ).toString( 16 ).padLeft( 32, '0' )
		}
	}
	
	@Test
	void groossShouldRespondOKToPing() {
		
		assert config.grooss.tokens.api.id
		def grooss = new Grooss( apiToken: [id: config.grooss.tokens.api.id ] )
		assert grooss.ping() == "ok"
	}
	
	@Test
    void groossShouldReturnRequestTokenAndURL() {
        
		def grooss = new Grooss()
        assert grooss.oauthToken.secret
        assert grooss.oauthToken.id
    }
    
	@Test
    void accessTokenShouldFailUnauthorized() {
		
		def grooss = new Grooss()
        grooss.getRequestToken()
        grooss.getAccessToken().with {
            assert stat    == "fail"
            assert message == "invalid/expired token" 
        }
    }
    
	@Test
    void defaultAuthorizationShouldWork() {
		
		def grooss = new Grooss()		
        if( grooss.oauthToken.id )
            assert "ok" == grooss.checkAccessToken().stat
    }
	
	@Test
	void getImagesShouldFetchProperNumberOfImageRecords() {
		
		def grooss = new Grooss()
		assert 4 == grooss.getImages( AlbumID: '4982718', AlbumKey: 'VL2Sdx' ).size()
	}
	
	@Test
	void imageInfoShouldBeReceivedOnExistingImage() {
		
		def grooss = new Grooss()
		assert 'JPG' == grooss.getImageInfo( ImageID: '298563241', ImageKey: 'JQeBJ').Format
	}

	@Test
	void getImageInfoShouldProvideDownloadableURL() {
		
		def grooss = new Grooss()
		def image = grooss.getImageInfo( ImageID: '298563241', ImageKey: 'JQeBJ' )
		def file  = grooss.downloadOriginal( image )
		assert file.md5 == image.MD5Sum
		file.delete()
	}
	
}
