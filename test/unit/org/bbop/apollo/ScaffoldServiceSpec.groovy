package org.bbop.apollo

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ScaffoldService)
@Mock([Sequence,FeatureLocation,Feature,Gene,MRNA,Exon,CDS,OverlapperService,TranscriptService,ExonService,FeatureRelationshipService])
class ScaffoldServiceSpec extends Specification {
	
    def setup() {
    	Sequence sequence = new Sequence(
    		name: 'KB0001',
    		start: 1,
    		end: 100,
    		length: 99,
    	).save(failOnError: true)
    	
    	geneBuilder(sequence,'GeneA',1,30)
    	geneBuilder(sequence,'GeneB',40,70)
      	geneBuilder(sequence,'GeneC',60,90)    	    	
    }
    


    def cleanup() {
    }

    void "test scan"() {
    	
    	given:
    		List input = [1,2,2,3,4,4,4,4,5,5,5,6]
    		List results = []
    		def check = {valueOne,valueTwo -> 
    			if(valueOne == valueTwo){
    				results << valueOne
    			}
    		}
    	when:
    		service.scan(0,1,input,check)
    	then:
    		results == [2,4,4,4,4,4,4,5,5,5]
    }
    
    void "get all exon on scaffold"() {
    	given:
    		Sequence sequence = Sequence.findByName("KB0001")
    	when:
    		List results  = service.getExonsOnScaffold(sequence,1)
    	then:
    		results.size() == 6
    }
    
    void "scan scaffold for overlapping Exons"(){
    	given:
    		Map exonOverlap = [GeneBExon2:1,GeneCExon1:1]
    		Sequence sequence = Sequence.findByName("KB0001")
    		List exons  = service.getExonsOnScaffold(sequence,1)
    	when:
    		def results = service.exonOverlapScan(exons)
    	then:
    		exonOverlap == results
    		
    
    }
    
    void "mapExonToGene"(){
    	given:
    		Map mergeCandidats = [GeneBExon2:1,GeneCExon1:1]
    		Map geneOverlap = [GeneB:1,GeneC:1]
    		def exons = Exon.getAll() // Needed to innitiate the test Database
    	when:
    		Map results = service.mapExonToGene(mergeCandidats)
    	then:
    		results.size() == 1
    		results.get(1) instanceof Set
    		results.get(1).size() == 2
    }
    
    void "remove sets with only one gene"(){
    	given:
    		def exons = Exon.getAll() // Needed to innitiate the test Database
    		Map exonGroups = [GeneBExon2:1,GeneCExon1:1]
    		Map mergeCandidats = service.mapExonToGene(exonGroups)
    	when:
    		Map results = service.deleteSingelGenes(mergeCandidats)
    	then:
    		results.size() == 1
    }
    
    /*
    void "Check that all transcript is in same reading frame"(){
    	given:
    		def exons = Exon.getAll() // Needed to innitiate the test Database
    		Map exonGroups = [GeneBExon2:1,GeneCExon1:1]
    		Map mergeCandidats = service.mapExonToGene(exonGroups)
    	when:
    		Map results = service.checkReadingFrame(mergeCandidats)
    	then:
    		
    }
    */
    
    
    def geneBuilder(Sequence sequence,String uniquename, int begin, int end) {
        
    	Gene gene = new Gene(
    		name: uniquename,
    		uniqueName: uniquename
    	).save(failOnError: true)
    	
       FeatureLocation geneLocation = new FeatureLocation(
			 fmin: begin,
			 fmax: end,
			 feature: gene,
			 sequence: sequence,
			 strand: 1,
        ).save()
        
        gene.addToFeatureLocations(geneLocation)
        gene.save()
    	
    	MRNA mrna = new MRNA(
    		name: uniquename + "MRNA",
    		uniqueName: uniquename + "MRNA" 
    	).save(failOnError: true)
    	
    	FeatureLocation mrnaLocation = new FeatureLocation(
    		fmin: begin,
    		fmax: end,
    		feature: mrna,
    		sequence: sequence,
    		strand: 1,
    	).save()
    	
    	mrna.addToFeatureLocations(mrnaLocation)
        mrna.save()
    	
        FeatureRelationship GeneToMRNA =new FeatureRelationship(
                parentFeature: gene,
                childFeature: mrna
        ).save(failOnError: true)
        mrna.addToChildFeatureRelationships(GeneToMRNA)
        gene.addToParentFeatureRelationships(GeneToMRNA)        
        
        Exon exon1 = new Exon(
    		name: uniquename + "Exon1",
    		uniqueName: uniquename + "Exon1"
    	).save(failOnError: true)    	
    	
    	FeatureLocation exon1Location = new FeatureLocation(
    		fmin: begin,
    		fmax: begin + 10,
    		feature: exon1,
    		sequence: sequence,
    		strand: 1,
    	).save()    	
    	exon1.addToFeatureLocations(exon1Location)
        exon1.save()
        
        FeatureRelationship MRNAToExon1 =new FeatureRelationship(
        	parentFeature: mrna,
            childFeature: exon1
        ).save(failOnError: true)
        exon1.addToChildFeatureRelationships(MRNAToExon1)
        mrna.addToParentFeatureRelationships(MRNAToExon1)
    	
    	Exon exon2 = new Exon(
    		name: uniquename + "Exon2",
    		uniqueName: uniquename + "Exon2"
    	).save(failOnError: true)
    	
    	FeatureLocation exon2Location = new FeatureLocation(
    		fmin: begin + 20,
    		fmax: begin + 30,
    		feature: exon2,
    		sequence: sequence,
    		strand: 1,
    	).save()    	
    	exon2.addToFeatureLocations(exon2Location)
        exon2.save()

        FeatureRelationship MRNAToExon2 =new FeatureRelationship(
        	parentFeature: mrna,
            childFeature: exon2
        ).save(failOnError: true)
        exon2.addToChildFeatureRelationships(MRNAToExon2)
        mrna.addToParentFeatureRelationships(MRNAToExon2)  
    
    
    }    
}
