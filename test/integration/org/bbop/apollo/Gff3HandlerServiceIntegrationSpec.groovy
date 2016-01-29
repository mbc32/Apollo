package org.bbop.apollo

import grails.test.spock.IntegrationSpec
import grails.converters.JSON
class Gff3HandlerServiceIntegrationSpec extends IntegrationSpec {
   
    def gff3HandlerService
    def requestHandlingService

    def setup() {
        Organism organism = new Organism(
                directory: "test/integration/resources/sequences/honeybee-Group1.10/"
                , commonName: "honeybee"
        ).save(flush: true)
        Sequence sequence = new Sequence(
                length: 1405242
                , seqChunkSize: 20000
                , start: 0
                , organism: organism
                , end: 1405242
                , name: "Group1.10"
        ).save()
    }

    def cleanup() {
    }

    void "write a GFF3 of a simple gene model"() {


        given: "we create a new gene"
        String json=' { "track": "Group1.10", "features": [{"location":{"fmin":1216824,"fmax":1235616,"strand":1},"type":{"cv":{"name":"sequence"},"name":"mRNA"},"name":"GB40856-RA","children":[{"location":{"fmin":1235534,"fmax":1235616,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":1216824,"fmax":1216850,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":1224676,"fmax":1224823,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":1228682,"fmax":1228825,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":1235237,"fmax":1235396,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":1235487,"fmax":1235616,"strand":1},"type":{"cv":{"name":"sequence"},"name":"exon"}},{"location":{"fmin":1216824,"fmax":1235534,"strand":1},"type":{"cv":{"name":"sequence"},"name":"CDS"}}]}], "operation": "add_transcript" }'
        
        
        when: "we parse the json"
        requestHandlingService.addTranscript(JSON.parse(json))
        

        then: "We should have at least one new gene"
        assert Gene.count == 1
        assert MRNA.count == 1
        assert Exon.count == 5
        assert CDS.count == 1


        when: "we write the feature to test"
        File tempFile = File.createTempFile("output", ".gff3")
        tempFile.deleteOnExit()
        log.debug "${tempFile.absolutePath}"
        def featuresToWrite = Gene.findAll()
        log.debug "${featuresToWrite}"
        gff3HandlerService.writeFeaturesToText(tempFile.absolutePath,featuresToWrite,".")
        String tempFileText = tempFile.text
        log.debug "${tempFile.text}"

        then: "we should get a valid gff3 file"
        log.debug "${tempFileText}"
        def lines = tempFile.readLines()
        assert lines[0]=="##gff-version 3"
        assert lines[2].split("\t")[2]=="gene"
        assert lines[2].split("\t")[8].indexOf("ID=abc123")!=-1
        assert lines[2].split("\t")[8].indexOf("Name=Bob")!=-1
        assert lines[3].split("\t")[2]=="mRNA"
        assert lines[11].split("\t")[2]=="insertion"
        assert lines[11].split("\t")[8].indexOf("justification=Sanger sequencing")!=-1
        assert tempFileText.length() > 0
    }
}
