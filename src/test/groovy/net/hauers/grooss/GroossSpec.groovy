package net.hauers.grooss

// @Grab(group='org.spockframework', module='spock-core', version='0.7-groovy-2.0')
// @Grab(group='org.codehaus.groovy.modules.http-builder', module='http-builder', version= '0.6')
// @Grab(group='commons-lang', module= 'commons-lang', version= '2.4')

import spock.lang.*

class GroossSpec extends Specification {

    Grooss grooss
    
    def setup() {
        grooss = new Grooss()
    }

    def defaultAuthorizationShouldWork() {
        
        expect:
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
}
