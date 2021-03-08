package org.bbop.apollo

import grails.transaction.Transactional

@Transactional(readOnly = true)
class ScaffoldService {
	
	def overlapperService
	def transcriptService
	def exonService
	
    def serviceMethod() {

    }
    
    public Map exonOverlapScan(List exons){
    	Map mergeCandidats = [ : ]
    	int group = 0    	
    	Closure check = {leftExon,rightExon ->
    		if(overlapperService.overlaps(leftExon,rightExon)){
    			if(mergeCandidats.containsKey(leftExon.uniqueName)){
    				mergeCandidats[rightExon.uniqueName] = group
    			}else{
    				mergeCandidats[leftExon.uniqueName] = ++group
    				mergeCandidats[rightExon.uniqueName] = group
    			}
    			return 1
    		}else{return 0}
    	}
    	    	
    	scan(0,1,exons,check)
    	return mergeCandidats   
    }
    
    public Map mapExonToGene(Map exonGroups){
    	Map mergeCandidats = [ : ]
    	exonGroups.each{key,value ->
    		Set genes = []
    		Exon exon = Exon.findByUniqueName(key)
    		Gene gene = transcriptService.getGene(exonService.getTranscript(exon))
    		if(mergeCandidats.containsKey(value)){
    			mergeCandidats.get(value) << gene
    		}else{
    			genes << gene
    			mergeCandidats.put(value,genes)
    		}     		
    	}
	    return mergeCandidats
    }
    
    public Map deleteSingelGenes(Map mergeCandidats) {
    	mergeCandidats.each{key,value ->
    		if(value.size() == 1){
    			mergeCandidats.remove(key)
    		}	
    	}
    	return mergeCandidats
    }
    
    /*
    public Map checkReadingFrame(Map mergeCandidats){
    	List geneSets = []
    	Closure check = {gene1,gene2 ->
    		List transcripts1 =
    		List transcripts2 = 
    		
    	
    	}
    	
    	mergeCandidates.each{key,value ->
    		List overlappingGenes = []
    		overlappingGenes.addAll(value)
    		scan(0,1,overlappingGenes,check)
    	}	
    }
    */
    public List<Exon> getExonsOnScaffold(Sequence sequence, int strand){
    	List exons = []	  
    	sequence.getFeatureLocations().each{ featureLocation ->
    		  	  if(featureLocation.feature.ontologyId == Exon.ontologyId && featureLocation.strand == strand){
    		  	  	  exons << featureLocation.feature
    		  	  }    		  
    	}
    	return exons
    }
    
    public scan(int i=0, int j=1, List input,Closure rules){
    	if(input.size() == j){ //break condition    	  
    		return 0
    	}

    	if(rules(input[i],input[j])){
    		j++
    		scan(i,j,input,rules)
    	}else{
    		i++
    		j = i + 1
    		scan(i,j,input,rules)
    	}    	
    }
}
