package org.bbop.apollo

/**
 * NOTE: superclass is NOT region . . .
 */
import groovy.transform.ToString
@ToString 
class Exon extends TranscriptRegion{

    static constraints = {
    }

    static String cvTerm = "exon"
    static String ontologyId = "SO:0000147"// XX:NNNNNNN
    static String alternateCvTerm = "Exon"

}
