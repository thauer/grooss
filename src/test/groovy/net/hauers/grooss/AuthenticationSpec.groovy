package net.hauers.grooss

// @Grab(group='org.spockframework', module='spock-core', version='0.7-groovy-2.0')
// @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version= '0.7.1')
// @Grab(group='commons-lang', module= 'commons-lang', version= '2.4')

import spock.lang.*

class AuthenticationSpec extends Specification {

    def groossShouldRespondOKToPing() {
        when: "Parsing test-config.groovy off the classpath for the apiid config parameter"
        Class scriptClass = GroossSpec.class.classLoader.loadClass('test-config')
        ConfigObject config = new ConfigSlurper().parse(scriptClass)
        def apiid=config?.grooss.tokens.api.id
        then: "No exception is thrown and nonempty apiid is found"
        notThrown(Exception)
        apiid

        when: "Calling the ping() function of the interface initialized with the apiid"
        def grooss = new Grooss(apiToken: [id: apiid])
        def retval = grooss.ping()
        then: "The return is 'ok'"
        retval == "ok"
    }

    def groossShouldHaveOauthToken() {
        given: "A grooss interface initialized with the default ~/.grooss-config.groovy"
        def grooss = new Grooss()
        
        expect: "The interface has a nonempty oauthToken picked up from the config file"
        grooss.oauthToken.secret
        grooss.oauthToken.id
    }

    def accessTokenShouldFailUnauthorized() {
        
        when: "Getting an access token without the authorization step after request token"
        def token = new Grooss().with{
            requestToken
            accessToken
        }

        then: "The token request fails"
        token.stat    == "fail"
        token.message == "invalid/expired token" 
    }
    
    def defaultAuthorizationShouldWork() {

        when: "A grooss interface initialized with proper access tokens (from ~/.grooss-config.groovy)"
        def grooss = new Grooss()

        then: "The interface has a valid oauthToken" 
        grooss.oauthToken.id
        and: "The call to verify the validity of the token returns 'ok'"
        "ok" == grooss.checkAccessToken().stat
    }
}
