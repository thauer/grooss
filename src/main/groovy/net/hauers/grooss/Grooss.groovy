package net.hauers.grooss

import org.apache.commons.lang.RandomStringUtils
import groovyx.net.http.HTTPBuilder

class Grooss extends HTTPBuilder {

    static def apiURL    = 'https://api.smugmug.com/services/api/json/1.3.0/'
    static def authzURL  = 'https://secure.smugmug.com/services/oauth/authorize.mg'

    static File configFile = new File( 
        "${System.getProperty( 'user.home' )}/.grooss-config.groovy" )
    def apiToken   = [ id:'', secret:'' ]
    def oauthToken = [ id:'', secret:'' ]

    /**
     * Constructor.
     */
    Grooss() {
    
        super( apiURL )
        headers    = [ 'User-Agent' : 'Grooss/0.1 alpha', Accept : 'application/json' ]
		if( configFile.exists() ) {
	        def config = new ConfigSlurper().parse( configFile.toURI().toURL() )
	        apiToken   = [ id    : config.grooss.tokens.api.id
	                     , secret: config.grooss.tokens.api.secret ]
	        oauthToken = [ id    : config.grooss.tokens.default?.id
	                     , secret: config.grooss.tokens.default?.secret ]
		}
    }
    
    /**
     * Wrapper to do JSON communication on path='' with the APIKey an oauth magic
     */
    @Override
    def get( Map params ) {
    
        super.get( path : '', query: [ APIKey : apiToken.id
            , oauth_version          : '1.0'
            , oauth_timestamp        : (int) System.currentTimeMillis() / 1000             
            , oauth_nonce            : RandomStringUtils.randomAscii( 10 )
            , oauth_signature_method : 'PLAINTEXT'
            , oauth_signature        : "${apiToken.secret}&${oauthToken.secret}"
            , oauth_consumer_key     : apiToken.id
            , oauth_token            : oauthToken.id ]
            + params ){ r, json -> 
                
            assert r.success
            json
        }
    }
    
    /**
     * Ping: the simplest method to verify that the server is accessible. Needs an apiKey only.
     */
    def ping() {

        get( method: 'smugmug.service.ping' ).stat
    }
    
    /**
     * OAuth magic:
     * 1. getRequestToken() creates a [Secret:..., id:...] short-lived token
	 *    which is temporarily stored in the oauthToken  
     * 2. Using the url returned by getRequestToken, the oauthToken is authorized
     * 3. getAccessToken() uses the temporary oauthToken  to obtain the long-lived
	 *    access token:2 
     *    http://wiki.smugmug.net/display/API/show+1.3.0?method=smugmug.auth.getAccessToken
     */
    def getRequestToken() {
    
        oauthToken        = [ id: '', secret: '' ]
        def response      = get( method: 'smugmug.auth.getRequestToken' )
        oauthToken.secret = response.Auth?.Token?.Secret
        oauthToken.id     = response.Auth?.Token?.id
        response
    }
    
    def getAccessToken() {
    
        def response      = get( method: 'smugmug.auth.getAccessToken' )
		oauthToken.secret = response.Auth?.Token?.Secret
		oauthToken.id     = response.Auth?.Token?.id
        response
    }
    
	def checkAccessToken() {
		get( method: 'smugmug.auth.checkAccessToken' )		
	}
	
    def getAuthorization() {
             
        getRequestToken()
		def url = "${Grooss.authzURL}?oauth_token=${oauthToken.id}" +
								    "&Access=Full&Permissions=Modify" 
        System.in.withReader {
            print "Authorize at ${url}, Hit Enter once authorized [Enter]: "
            println it.readLine()
        }
		getAccessToken()
        println checkAccessToken()
    }
	
	/**
     * Functional methods
     */
	
	/**
	 * Retrieves all albums. See smugmug.albums.get.
	 * 
	 * @return Albums:[ [id:, Key:, Title:, Category:[id:, Name:], SubCategory:[id:, Name:]],...]
	 */
	def getAlbums() {
        get( method: 'smugmug.albums.get' ).Albums
    }
	
	/**
	 * Lists all images in an album
	 * 
	 * @param Required params are AlbumID and AlbumKey. See smugmug.images.get. 
	 * @return An array of image hashmaps
	 */
	def getImages( Map params ) {
		get( [ method: 'smugmug.images.get' ] + params ).Album.Images
	}
	
	/**
	 * Retrieves the details of a single image
	 * 
	 * @param params Required params are ImageID and ImageKey. See smugmug.images.getInfo. 
	 * @return A hashmap of a single image
	 */
	def getImageInfo( Map params ) {
		get( [ method: 'smugmug.images.getInfo'] + params ).Image
	}
	
	/**
	 * Downloads an image to a temporary file (with name suffix equals the original FileName)
	 * @param params Required params are ImageID and ImageKey.
	 * @return
	 */
	def downloadOriginal( Map image ) {
	
		if( ! image.FileName || ! image.OriginalURL )
			image = getImageInfo( ImageID: image.id, ImageKey: image.Key)

		def file = File.createTempFile("smug.", ".${image.FileName}" )
		file.newOutputStream().with{ it << new URL( image.OriginalURL ).openStream(); close() }
		file
	}    
}
