package net.hauers.grooss

// @Grab(group='org.spockframework', module='spock-core', version='0.7-groovy-2.0')
// @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version= '0.6')
// @Grab(group='commons-lang', module= 'commons-lang', version= '2.4')

import spock.lang.*

class AuthenticationSpec extends Specification {

    def groossShouldRespondOKToPing() {
        when:
        ConfigObject config = null
        try {
            Class scriptClass = GroossSpec.class.classLoader.loadClass('test-config')
            config = new ConfigSlurper().parse(scriptClass)
        } catch(e){ e.printStackTrace() }
        def apiid=config?.grooss.tokens.api.id
        then:
        apiid
        when:
        def grooss = new Grooss(apiToken: [id: apiid])
        then:
        grooss.ping() == "ok"
    }

    def groossShouldReturnRequestTokenAndURL() {
        def grooss = new Grooss()
        expect:
        grooss.oauthToken.secret
        grooss.oauthToken.id
    }

    def accessTokenShouldFailUnauthorized() {
        
        when:
        def token = new Grooss().with{
            requestToken
            accessToken
        }
        then:
        token.stat    == "fail"
        token.message == "invalid/expired token" 
    }
    
    def defaultAuthorizationShouldWork() {
        def grooss = new Grooss()
        expect:
        grooss.oauthToken.id
        "ok" == grooss.checkAccessToken().stat
    }
}
