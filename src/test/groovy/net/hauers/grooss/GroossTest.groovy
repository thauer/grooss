package net.hauers.grooss

import org.junit.Test
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
}
