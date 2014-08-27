package netparser.conversion.drugbank;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import netparser.conversion.graph.Graph;
import netparser.conversion.graph.Graph.Edge;
import netparser.conversion.graph.Graph.Node;
import netparser.conversion.graph.XGMMLWriter;
import netparser.conversion.utils.Utils;

import org.bridgedb.DataSource;
import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.Xref;
import org.jdom.JDOMException;

public class DrugBankParser {

	public static void main(String [] args) throws JDOMException, IOException {
		File file = new File("../resources/drugbank/drugbank4.xml");
		
		IDMapper mapper = Utils.initIDMapper(new File("/home/martina/Data/BridgeDb/Hs_Derby_20130701.bridge"), false);
		
		DrugBankXmlParser parser = new DrugBankXmlParser();
		Set<DrugModel> drugs = parser.parse(file);
		
		boolean approved = false;
		
		Graph graph = new Graph();
		if(!approved) {
			graph.setTitle("DrugBank v4");
		} else {
			graph.setTitle("DrugBank v4 approved drugs");
		}
		if(!approved) {
			graph.appendAttribute("Database", "DrugBank v4");
		} else {
			graph.appendAttribute("Database", "DrugBank v4 approved drugs");
		}
		graph.appendAttribute("URL", "http://www.drugbank.ca/system/downloads/current/drugbank.xml.zip");
		
		int countDrugs = 0;
		int countTargets = 0;
		int countEdges = 0;
		Set<String> edges = new HashSet<String>();
		for(DrugModel drug : drugs) {
			if(!approved || drug.getGroups().contains("approved")) {
				Node drugNode = createDrugNode(drug, graph);
				countDrugs++;
				for(TargetModel target : drug.getTargets()) {
					Node targetNode;
					if(graph.getNode(target.getDrugbankId()) == null) {
						targetNode = createTargetNode(target, graph, mapper);
						countTargets++;
					} else {
						targetNode = graph.getNode(target.getDrugbankId());
					}
					String edgeId = drugNode.getId() + "-" + targetNode.getId();
					if(!edges.contains(edgeId)) {
						Edge e = graph.addEdge(edgeId, drugNode, targetNode);
						countEdges++;
						e.appendAttribute("datasource", "DrugBank v4");
						e.appendAttribute("interactionType", "drug-target");
						edges.add(edgeId);
					}
				}
			}
		}
		
		if(!approved) {
			XGMMLWriter.write(graph, new PrintWriter(new File("drugbank-v4-complete.xgmml")));
		} else {
			XGMMLWriter.write(graph, new PrintWriter(new File("drugbank-v4-approved.xgmml")));
		}
		System.out.println("DrugBank network was created with " + countDrugs + " drugs, " + countTargets + " targets and " + countEdges + " drug-target interactions.");
	}
	
	private static Node createTargetNode(TargetModel target, Graph graph, IDMapper mapper) {
		System.out.println("create target node for " + target.getDrugbankId() + "\t" + target.getUniprotId());
		Node targetNode = graph.addNode(target.getDrugbankId());
		targetNode.appendAttribute("UniProt", target.getUniprotId());
		targetNode.appendAttribute("GeneName", target.getGeneName());
		targetNode.appendAttribute("name", target.getGeneName());
		targetNode.appendAttribute("Organism", target.getOrganism());
		targetNode.appendAttribute("biologicalType", "target");
		
		try {
			String identifiers = "[" + target.getUniprotId();
			Set<Xref> res = mapper.mapID(new Xref(target.getUniprotId(),  DataSource.getBySystemCode("S")), DataSource.getBySystemCode("En"));
			for(Xref x : res) {
				identifiers = identifiers + "," + x.getId();
			}
			Set<Xref> resEntrez = mapper.mapID(new Xref(target.getUniprotId(),  DataSource.getBySystemCode("S")), DataSource.getBySystemCode("L"));
			for(Xref x : resEntrez) {
				identifiers = identifiers + "," + x.getId();
			}
			Set<Xref> resHGNC = mapper.mapID(new Xref(target.getUniprotId(),  DataSource.getBySystemCode("S")), DataSource.getBySystemCode("H"));
			for(Xref x : resHGNC) {
				identifiers = identifiers + "," + x.getId();
			}
			identifiers = identifiers + "]";
			targetNode.appendAttribute("identifiers", identifiers);
		} catch (IDMapperException e) {
			e.printStackTrace();
		}
		
		return targetNode;
	}

	private static Node createDrugNode(DrugModel drug, Graph graph) {
		System.out.println("create drug node for " + drug.getDrugbankID());
		Node drugNode = graph.addNode(drug.getDrugbankID());
		drugNode.appendAttribute("DrugBankID", drug.getDrugbankID());
		drugNode.appendAttribute("CAS", drug.getCasNumber());
		drugNode.appendAttribute("DrugName", drug.getName());
		drugNode.appendAttribute("name", drug.getDrugbankID());
		drugNode.appendAttribute("Indication", drug.getIndication());
		drugNode.appendAttribute("InChiKey", drug.getInChiKey());
		drugNode.appendAttribute("biologicalType", "drug");
		String identifiers = "[" + drug.getDrugbankID();
		if(drug.getCasNumber() != null && !drug.getCasNumber().equals("")) {
			identifiers = identifiers + "," + drug.getCasNumber();
		}
		identifiers = identifiers + "]";
		drugNode.appendAttribute("identifiers", identifiers);
		
		String categories = "";
		for(String c : drug.getCategories()) {
			categories = categories + c + ", ";
		}
		if(categories.length() > 1) categories = categories.substring(0, categories.length()-2);
		drugNode.appendAttribute("categories", categories);
		
		String groups = "";
		for(String c : drug.getGroups()) {
			groups = groups + c + ", ";
		}
		if(groups.length() > 1) groups = groups.substring(0, groups.length()-2);
		drugNode.appendAttribute("groups", groups);
		return drugNode;
	}
	
}
