package net.hauers.grooss

import groovy.util.GroovyTestCase

class GroossTest extends GroovyTestCase {

    def grooss = null

    void setUp() {
        super.setUp()
        
        grooss = new Grooss()
    }

    void testTestingShouldWork() {
    
        assert 1 == 1
    }
    
    void testGroossClassShouldBeLoadable() {
    
        assertNotNull( Grooss.class )
    }
    
    void testGroosShouldRespondOKToPing() {
        
        assert "ok" == grooss.ping()  
    }
    
    void testGroossShouldReturnRequestTokenAndURL() {
        
        assert grooss.oauthToken.secret
        assert grooss.oauthToken.id
    }
    
    void testAccessTokenShouldFailUnauthorized() {
        grooss.getRequestToken()
        grooss.getAccessToken().with {
            assert stat    == "fail"
            assert message == "invalid/expired token" 
        }
    }
    
    void testDefaultAuthorizationShouldWork() {
        if( grooss.oauthToken.id )
            assert "ok" == grooss.checkAccessToken().stat
    }
}
