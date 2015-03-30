package org.fogbowcloud.manager.core.plugins.opennebula;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.codec.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.RequirementsHelper;
import org.fogbowcloud.manager.core.model.Flavor;
import org.fogbowcloud.manager.core.model.ResourcesInfo;
import org.fogbowcloud.manager.core.plugins.ComputePlugin;
import org.fogbowcloud.manager.occi.core.Category;
import org.fogbowcloud.manager.occi.core.ErrorType;
import org.fogbowcloud.manager.occi.core.OCCIException;
import org.fogbowcloud.manager.occi.core.Resource;
import org.fogbowcloud.manager.occi.core.ResourceRepository;
import org.fogbowcloud.manager.occi.core.ResponseConstants;
import org.fogbowcloud.manager.occi.core.Token;
import org.fogbowcloud.manager.occi.instance.Instance;
import org.fogbowcloud.manager.occi.instance.Instance.Link;
import org.fogbowcloud.manager.occi.request.RequestAttribute;
import org.fogbowcloud.manager.occi.request.RequestConstants;
import org.opennebula.client.Client;
import org.opennebula.client.OneResponse;
import org.opennebula.client.group.Group;
import org.opennebula.client.image.Image;
import org.opennebula.client.image.ImagePool;
import org.opennebula.client.template.Template;
import org.opennebula.client.template.TemplatePool;
import org.opennebula.client.user.User;
import org.opennebula.client.vm.VirtualMachine;
import org.opennebula.client.vm.VirtualMachinePool;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Status;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class OpenNebulaComputePlugin implements ComputePlugin {

	public static final int VALUE_DEFAULT_QUOTA_OPENNEBULA = -1;
	public static final int VALUE_UNLIMITED_QUOTA_OPENNEBULA = -2;
	public static final int VALUE_DEFAULT_MEM = 20480; // 20 GB
	public static final int VALUE_DEFAULT_CPU = 100;
	public static final int VALUE_DEFAULT_VMS = 100;
	private OpenNebulaClientFactory clientFactory;
	private String openNebulaEndpoint;
	private Map<String, String> fogbowTermToOpenNebula; 
	private String networkId;
	
	private String sshHost;
	private Integer sshPort;
	private String sshUsername;
	private String sshKeyFile;
	private String sshTargetTempFolder;
	private Integer dataStoreId;
	private List<Flavor> flavors;
	private String templateType;
	private List<String> validTemplates;
	
	private static final Logger LOGGER = Logger.getLogger(OpenNebulaComputePlugin.class);


	public OpenNebulaComputePlugin(Properties properties){
		this(properties, new OpenNebulaClientFactory());
	}
		
	public OpenNebulaComputePlugin(Properties properties, OpenNebulaClientFactory clientFactory) {
		this.openNebulaEndpoint = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_URL);
		this.clientFactory = clientFactory;
		fogbowTermToOpenNebula = new HashMap<String, String>();
		
		if (properties.get(OneConfigurationConstants.COMPUTE_ONE_NETWORK_KEY) == null){
			throw new OCCIException(ErrorType.BAD_REQUEST,
					ResponseConstants.NETWORK_NOT_SPECIFIED);			
		}		
		networkId = String.valueOf(properties.get(OneConfigurationConstants.COMPUTE_ONE_NETWORK_KEY));
		
		sshHost = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_SSH_HOST);
		String sshPortStr = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_SSH_PORT);
		sshPort = sshPortStr == null ? null : Integer.valueOf(sshPortStr);
		
		sshUsername = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_SSH_USERNAME);
		sshKeyFile = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_SSH_KEY_FILE);
		sshTargetTempFolder = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_SSH_TARGET_TEMP_FOLDER);
		
		String dataStoreIdStr = properties.getProperty(OneConfigurationConstants.COMPUTE_ONE_DATASTORE_ID);
		dataStoreId = dataStoreIdStr == null ? null: Integer.valueOf(dataStoreIdStr);
		
		validTemplates = new ArrayList<String>();

		templateType = properties.getProperty(OneConfigurationConstants.OPENNEBULA_TEMPLATES);
		if (templateType != null
				&& !templateType.equals(OneConfigurationConstants.OPENNEBULA_TEMPLATES_TYPE_ALL)) {
			validTemplates = getTemplatesInProperties(properties);
		}
		
		flavors = new ArrayList<Flavor>();
		
		// userdata
		fogbowTermToOpenNebula.put(RequestConstants.USER_DATA_TERM, "user_data");
		
		//ssh public key
		fogbowTermToOpenNebula.put(RequestConstants.PUBLIC_KEY_TERM, "ssh-public-key");
	}

	@Override
	public String requestInstance(Token token, List<Category> categories,
			Map<String, String> xOCCIAtt, String localImageId) {
		
		LOGGER.debug("Requesting instance with token=" + token + "; categories="
				+ categories + "; xOCCIAtt=" + xOCCIAtt);
		
		Map<String, String> templateProperties = new HashMap<String, String>();
		
		// removing fogbow-request category
		categories.remove(new Category(RequestConstants.TERM, RequestConstants.SCHEME,
				RequestConstants.KIND_CLASS));			
		
		Flavor foundFlavor = getFlavor(token, xOCCIAtt.get(RequestAttribute.REQUIREMENTS.getValue()));
		
		// checking categories are valid	
		for (Category category : categories) {
			if (category.getScheme().equals(RequestConstants.TEMPLATE_RESOURCE_SCHEME)) {
				continue;
			}
			
			if (fogbowTermToOpenNebula.get(category.getTerm()) == null) {
				throw new OCCIException(ErrorType.BAD_REQUEST,
						ResponseConstants.CLOUD_NOT_SUPPORT_CATEGORY + category.getTerm());
			} 
			
			if (category.getTerm().equals(RequestConstants.PUBLIC_KEY_TERM)) {
				templateProperties.put("ssh-public-key",
						xOCCIAtt.get(RequestAttribute.DATA_PUBLIC_KEY.getValue()));
			}
		}		
		
		// image or flavor was not specified
		if (foundFlavor == null || localImageId == null){
			throw new OCCIException(ErrorType.BAD_REQUEST,
					ResponseConstants.IRREGULAR_SYNTAX);
		}
		
		String userdata = xOCCIAtt.get(RequestAttribute.USER_DATA_ATT.getValue());
		if (userdata != null){
			userdata = normalizeUserdata(userdata);
		}
		templateProperties.put("mem", String.valueOf(foundFlavor.getMem()));
		templateProperties.put("cpu", String.valueOf(foundFlavor.getCpu()));
		templateProperties.put("userdata", userdata);
		templateProperties.put("image-id", localImageId);
		templateProperties.put("disk-size", String.valueOf(foundFlavor.getDisk()));

		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		String vmTemplate = generateTemplate(templateProperties);	
		
		LOGGER.debug("The instance will be allocated according to template: " + vmTemplate);
		return clientFactory.allocateVirtualMachine(oneClient, vmTemplate);
	}

	public static String normalizeUserdata(String userdata) {
		userdata = new String(Base64.decodeBase64(userdata), Charsets.UTF_8);
		userdata = userdata.replaceAll("\n", "\\\\n");
		userdata = new String(Base64.encodeBase64(userdata.getBytes(Charsets.UTF_8), false, false),
				Charsets.UTF_8);
		return userdata;
	}
	
	private String generateTemplate(Map<String, String> templateProperties) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			// template elements
			Document doc = docBuilder.newDocument();
			Element templateElement = doc.createElement("TEMPLATE");
			doc.appendChild(templateElement);
			// context elements
			Element contextElement = doc.createElement("CONTEXT");
			templateElement.appendChild(contextElement);
			// ssh public key
			if (templateProperties.get("ssh-public-key") != null) {
				Element sshPublicKeyElement = doc.createElement("SSH_PUBLIC_KEY");
				sshPublicKeyElement.appendChild(doc.createTextNode(templateProperties
						.get("ssh-public-key")));
				contextElement.appendChild(sshPublicKeyElement);
			}
			// userdata
			String userdata = templateProperties.get("userdata");
			if (userdata != null) {
				Element userdataEncodingEl = doc.createElement("USERDATA_ENCODING");
				userdataEncodingEl.appendChild(doc.createTextNode("base64"));
				contextElement.appendChild(userdataEncodingEl);
				Element userdataElement = doc.createElement("USERDATA");
				userdataElement.appendChild(doc.createTextNode(userdata));
				contextElement.appendChild(userdataElement);
			}
			// cpu
			Element cpuElement = doc.createElement("CPU");
			cpuElement.appendChild(doc.createTextNode(templateProperties.get("cpu")));
			templateElement.appendChild(cpuElement);
			//graphics
			Element graphicsElement = doc.createElement("GRAPHICS");
			Element listenElement = doc.createElement("LISTEN");
			listenElement.appendChild(doc.createTextNode("0.0.0.0"));
			Element typeElement = doc.createElement("TYPE");
			typeElement.appendChild(doc.createTextNode("vnc"));
			graphicsElement.appendChild(listenElement);
			graphicsElement.appendChild(typeElement);
			templateElement.appendChild(graphicsElement);
			// disk
			Element diskElement = doc.createElement("DISK");
			templateElement.appendChild(diskElement);
			// image
			Element imageElement = doc.createElement("IMAGE_ID");
			imageElement.appendChild(doc.createTextNode(templateProperties.get("image-id")));
			diskElement.appendChild(imageElement);
			
			String diskSize = templateProperties.get("disk-size");
			if (!diskSize.equals("0")) {
				// disk volatile
				Element diskVolatileElement = doc.createElement("DISK");
				templateElement.appendChild(diskVolatileElement);
				// size
				Element sizeElement = doc.createElement("SIZE");
				sizeElement.appendChild(doc.createTextNode(diskSize));
				diskVolatileElement.appendChild(sizeElement);
				// type 
				Element typeElementDisk = doc.createElement("TYPE");
				typeElementDisk.appendChild(doc.createTextNode("fs"));
				diskVolatileElement.appendChild(typeElementDisk);
			}
			
			// memory
			Element memoryElement = doc.createElement("MEMORY");
			memoryElement.appendChild(doc.createTextNode(templateProperties.get("mem")));
			templateElement.appendChild(memoryElement);
			// nic
			Element nicElement = doc.createElement("NIC");
			templateElement.appendChild(nicElement);
			// network
			Element networkElement = doc.createElement("NETWORK_ID");
			networkElement.appendChild(doc.createTextNode(networkId));
			nicElement.appendChild(networkElement);			
			// getting xml template 
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			DOMSource source = new DOMSource(doc);
			
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			transformer.transform(source, result);
			return writer.toString();
		} catch (ParserConfigurationException e) {
			LOGGER.error("", e);
		} catch (TransformerConfigurationException e) {
			LOGGER.error("", e);
		} catch (TransformerException e) {
			e.printStackTrace();
			LOGGER.error("", e);
		}
		return "";
	}

	@Override
	public List<Instance> getInstances(Token token) {
		LOGGER.debug("Getting instances of token: " + token);

		List<Instance> instances = new ArrayList<Instance>();
		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		VirtualMachinePool vmPool = clientFactory.createVirtualMachinePool(oneClient);
		for (VirtualMachine virtualMachine : vmPool) {
			instances.add(mountInstance(virtualMachine));
		}
		return instances;
	}

	@Override
	public Instance getInstance(Token token, String instanceId) {
		LOGGER.debug("Getting instance " + instanceId + " of token: " + token);

		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		VirtualMachine vm = clientFactory.createVirtualMachine(oneClient, instanceId);
		return mountInstance(vm);
	}

	private Instance mountInstance(VirtualMachine vm) {
		LOGGER.debug("Mounting instance structure of instanceId: " + vm.getId());

		String mem = vm.xpath("TEMPLATE/MEMORY");
		String cpu = vm.xpath("TEMPLATE/CPU");
		String image = vm.xpath("TEMPLATE/DISK/IMAGE");
		String arch = vm.xpath("TEMPLATE/OS/ARCH");
		
		LOGGER.debug("mem=" + mem + ", cpu=" + cpu + ", image=" + image + ", arch=" + arch);

		// TODO To get information about network when it'll be necessary
		// vm.xpath("TEMPLATE/NIC/NETWORK");
		// vm.xpath("TEMPLATE/NIC/NETWORK_ID");

		Map<String, String> attributes = new HashMap<String, String>();
		// CPU Architecture of the instance
		attributes.put("occi.compute.architecture", getArch(arch));
		attributes.put("occi.compute.state", getOCCIState(vm.lcmStateStr()));
		// CPU Clock frequency (speed) in gigahertz
		attributes.put("occi.compute.speed", "Not defined");
		attributes.put("occi.compute.memory", String.valueOf(Double.parseDouble(mem) / 1024)); // Gb
		attributes.put("occi.compute.cores", cpu);
		attributes.put("occi.compute.hostname", vm.getName());
		attributes.put("occi.core.id", vm.getId());

		List<Resource> resources = new ArrayList<Resource>();
		resources.add(ResourceRepository.getInstance().get("compute"));
		resources.add(ResourceRepository.getInstance().get("os_tpl"));
		resources.add(ResourceRepository.getInstance().get(
				getUsedFlavor(Double.parseDouble(cpu), Double.parseDouble(mem))));
		
		return new Instance(vm.getId(), resources, attributes, new ArrayList<Link>());
	}

	private String getArch(String arch) {		
		// x86 is default
		return !arch.isEmpty() ? arch : "x86";
	}

	private String getOCCIState(String oneVMState) {
		if ("Running".equalsIgnoreCase(oneVMState)) {
			return "active";
		} else if ("Suspended".equalsIgnoreCase(oneVMState)){
			return "suspended";
		}
		return "inactive";
	}

	private String getUsedFlavor(double cpu, double mem) {
		for (Flavor flavor : flavors) {
			if (Double.parseDouble(flavor.getCpu()) == cpu && Double.parseDouble(flavor.getMem()) == mem) {
				return flavor.getName();
			}
		}
		return null;
	}

	@Override
	public void removeInstance(Token token, String instanceId) {
		LOGGER.debug("Removing instanceId " + instanceId + " with token " + token);
		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		VirtualMachine vm = clientFactory.createVirtualMachine(oneClient, instanceId);
		OneResponse response = vm.delete();
		if (response.isError()) {			
			LOGGER.error("Error while removing vm: " + response.getErrorMessage());
		}
	}

	@Override
	public void removeInstances(Token token) {
		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		VirtualMachinePool vmPool = clientFactory.createVirtualMachinePool(oneClient);
		for (VirtualMachine virtualMachine : vmPool) {
			OneResponse response = virtualMachine.delete();
			if (response.isError()) {
				LOGGER.error("Error while removing vm: " + response.getErrorMessage());
			}
		}
	}

	@Override
	public ResourcesInfo getResourcesInfo(Token token) {
		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);				
		User user = clientFactory.createUser(oneClient, token.getUser());
		String groupId = user.xpath("GROUPS/ID");
		Group group = clientFactory.createGroup(oneClient, Integer.parseInt(groupId));

		String maxCpuStr = group.xpath("VM_QUOTA/VM/CPU");
		String cpuInUseStr = group.xpath("VM_QUOTA/VM/CPU_USED");
		String maxMemStr = group.xpath("VM_QUOTA/VM/MEMORY");
		String memInUseStr = group.xpath("VM_QUOTA/VM/MEMORY_USED");
		String maxVMsStr = group.xpath("VM_QUOTA/VM/VMS");
		String vmsInUseStr = group.xpath("VM_QUOTA/VM/VMS_USED");
		
		// default values is used when quota is not specified
		double maxCpu = VALUE_DEFAULT_CPU;
		double cpuInUse = 0;
		double maxMem = VALUE_DEFAULT_MEM;
		double memInUse = 0;
		double maxVMs = VALUE_DEFAULT_VMS;
		double vmsInUse = 0;

		// getting quota values
		if (isValidDouble(maxCpuStr)) {
			maxCpu = Integer.parseInt(maxCpuStr);
		}
		if (isValidDouble(cpuInUseStr)) {
			cpuInUse = Integer.parseInt(cpuInUseStr);
		}
		if (isValidDouble(maxMemStr)) {
			maxMem = Integer.parseInt(maxMemStr);
		}
		if (isValidDouble(memInUseStr)) {
			memInUse = Integer.parseInt(memInUseStr);
		}
		if (isValidDouble(maxVMsStr)) {
			maxVMs = Integer.parseInt(maxVMsStr);
		}
		if (isValidDouble(vmsInUseStr)) {
			vmsInUse = Integer.parseInt(vmsInUseStr);
		}

		if (maxMem == VALUE_DEFAULT_QUOTA_OPENNEBULA) {
			maxMem = VALUE_DEFAULT_MEM;
		} else if (maxMem == VALUE_UNLIMITED_QUOTA_OPENNEBULA) {
			maxMem = Integer.MAX_VALUE;
		}

		if (maxCpu == VALUE_DEFAULT_QUOTA_OPENNEBULA) {
			maxCpu = VALUE_DEFAULT_CPU;
		} else if (maxCpu == VALUE_UNLIMITED_QUOTA_OPENNEBULA) {
			maxCpu = Integer.MAX_VALUE;
		}
		
		if (maxVMs == VALUE_DEFAULT_QUOTA_OPENNEBULA) {
			maxVMs = VALUE_DEFAULT_CPU;
		} else if (maxVMs == VALUE_UNLIMITED_QUOTA_OPENNEBULA) {
			maxVMs = Integer.MAX_VALUE;
		}

		double cpuIdle = maxCpu - cpuInUse;
		double memIdle = maxMem - memInUse;
		double instancesIdle = maxVMs - vmsInUse;
	
		return new ResourcesInfo(String.valueOf(cpuIdle), String.valueOf(cpuInUse),
				String.valueOf(memIdle), String.valueOf(memInUse), getFlavors(cpuIdle, memIdle, instancesIdle),
				null);
	}
	
	private boolean isValidDouble(String number) {
		try {
			Double.parseDouble(number);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private List<Flavor> getFlavors(double cpuIdle, double memIdle, double instancesIdle) {
		
		List<Flavor> newFlavorsList = new ArrayList<Flavor>();
		for (Flavor flavor : this.flavors) {
			int capacity = 0;
			try {
				double memFlavor = Double.parseDouble(flavor.getMem());
				double cpuFlavor = Double.parseDouble(flavor.getCpu());
				capacity = Math.min((int) Math.min(cpuIdle / cpuFlavor, memIdle / memFlavor),
						(int) instancesIdle);
			} catch (Exception e) {
				LOGGER.error("", e);
				throw new OCCIException(ErrorType.BAD_REQUEST,
						ResponseConstants.INVALID_FLAVOR_SPECIFIED);
			}
			flavor.setCapacity(capacity);
			newFlavorsList.add(flavor);
		}
		
		return newFlavorsList;
	}

	@Override
	public void bypass(Request request, Response response) {
		response.setStatus(new Status(HttpStatus.SC_BAD_REQUEST),
				ResponseConstants.CLOUD_NOT_SUPPORT_OCCI_INTERFACE);
	}

	@Override
	public void uploadImage(Token token, String imagePath, String imageName) {
		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		String remoteFilePath = sshTargetTempFolder + "/" + UUID.randomUUID();
		
		OpenNebulaSshClientWrapper sshClientWrapper = new OpenNebulaSshClientWrapper();
		try {
			sshClientWrapper.connect(sshHost, sshPort, sshUsername, sshKeyFile);
			sshClientWrapper.doScpUpload(imagePath, remoteFilePath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				sshClientWrapper.disconnect();
			} catch (IOException e) {
			}
		}
		
		Map<String, String> templateProperties = new HashMap<String, String>();
		templateProperties.put("image_name", imageName);
		templateProperties.put("image_path", remoteFilePath);
		Long imageSize = (long) Math.ceil(((double) new File(imagePath).length()) / (1024d * 1024d));
		templateProperties.put("image_size", imageSize.toString());
		OneResponse response = Image.allocate(oneClient, generateImageTemplate(templateProperties), dataStoreId);
		
		if (response.isError()) {
			throw new OCCIException(ErrorType.BAD_REQUEST, response.getErrorMessage());
		}
		
		Image.chmod(oneClient, response.getIntMessage(), 744);
	}
	
	private String generateImageTemplate(Map<String, String> templateProperties) {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("IMAGE");
			doc.appendChild(rootElement);
			
			Element nameElement = doc.createElement("NAME");
			nameElement.appendChild(doc.createTextNode(templateProperties.get("image_name")));
			rootElement.appendChild(nameElement);
			
			Element pathElement = doc.createElement("PATH");
			pathElement.appendChild(doc.createTextNode(templateProperties.get("image_path")));
			rootElement.appendChild(pathElement);
			
			Element sizeElement = doc.createElement("SIZE");
			sizeElement.appendChild(doc.createTextNode(templateProperties.get("image_size")));
			rootElement.appendChild(sizeElement);
			
			Element driverElement = doc.createElement("DRIVER");
			driverElement.appendChild(doc.createTextNode("qcow2"));
			rootElement.appendChild(driverElement);
			
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			
			DOMSource source = new DOMSource(doc);
			StringWriter stringWriter = new StringWriter();
			StreamResult result = new StreamResult(stringWriter);
			
			transformer.transform(source, result);
			
			return stringWriter.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getImageId(Token token, String imageName) {
		Client oneClient = clientFactory.createClient(token.getAccessId(), openNebulaEndpoint);
		ImagePool imagePool = new ImagePool(oneClient); 
		OneResponse response = imagePool.info();
		
		if (response.isError()) {
			throw new OCCIException(ErrorType.BAD_REQUEST, response.getErrorMessage());
		}
		
		for (Image image : imagePool) {
			if (image.getName().equals(imageName)) {
				return image.getId();
			}
		}
		return null;
	}

	public List<Flavor> getFlavors() {
		return this.flavors;
	}

	public void updateFlavors(Token token) {
		Client oneClient = this.clientFactory.createClient(token.getAccessId(),
				openNebulaEndpoint);
		List<Flavor> newFlavors = new ArrayList<Flavor>();		
		
		Map<String, String> mapImageToSize = new HashMap<String, String>();
		ImagePool imagePool = this.clientFactory.createImagePool(oneClient);
		for (Image image : imagePool) {
			mapImageToSize.put(image.getName(), image.xpath("SIZE"));
		}				
		
		TemplatePool templatePool = this.clientFactory.createTemplatePool(oneClient);
		for (Template template : templatePool) {
			String name = template.xpath("NAME");
			String memory = template.xpath("TEMPLATE/MEMORY");
			String vcpu = template.xpath("TEMPLATE/CPU");
						
			if (!validTemplates.isEmpty()) {
				boolean thereIsTemplate = false;
				for (String templateName : validTemplates) {
					if (templateName.equals(name)) {
						thereIsTemplate = true;
					}
				}
				if (!thereIsTemplate) {
					continue;
				}						
			}
			
			int cont = 1;
			int diskSize = 0;
			String templateDisk = null;
			do {
				String templateNameDisk = template.xpath("TEMPLATE/DISK[" + cont + "]/IMAGE");
				String templateDiskVolateSize = template.xpath("TEMPLATE/DISK[" + cont + "]/SIZE");
				if (templateDiskVolateSize != null && !templateDiskVolateSize.isEmpty()) {
					try {
						diskSize += Integer.parseInt(templateDiskVolateSize);
					} catch (Exception e) {
					}
				} else if (templateNameDisk != null && !templateNameDisk.isEmpty()){
					try {
						diskSize += Integer.parseInt(mapImageToSize.get(templateNameDisk));
					} catch (Exception e) {
					}
				}
				cont += 1;
				templateDisk = template.xpath("TEMPLATE/DISK[" + cont + "]");				
			} while (templateDisk != null && !templateDisk.isEmpty());

			newFlavors.add(new Flavor(name, vcpu, memory, String.valueOf(diskSize), 0));
		}
		if (newFlavors != null) {
			this.flavors.addAll(newFlavors);			
		}
		removeInvalidFlavors(newFlavors);
	}
	
	public void removeInvalidFlavors(List<Flavor> flavors) {
		ArrayList<Flavor> copyFlavors = new ArrayList<Flavor>(this.flavors);
		for (Flavor flavor : copyFlavors) {
			boolean containsFlavor = false;
			for (Flavor flavorName : flavors) {
				if (flavorName.getName().equals(flavor.getName())) {
					containsFlavor = true;
					continue;
				}
			}
			if (!containsFlavor && copyFlavors.size() != 0) {
				try {
					this.flavors.remove(flavor);					
				} catch (Exception e) {
				}
			}
		}
	}	
	
	public void setFlavors(List<Flavor> flavors) {
		this.flavors = flavors;
	}

	public Flavor getFlavor(Token token, String requirements) {
		if (templateType == null || (!templateType.equals(OneConfigurationConstants.OPENNEBULA_TEMPLATES_TYPE_ALL) && validTemplates.isEmpty())) {
			String cpu = RequirementsHelper.getValueSmallerPerAttribute(requirements, RequirementsHelper.GLUE_VCPU_TERM);
			String mem = RequirementsHelper.getValueSmallerPerAttribute(requirements, RequirementsHelper.GLUE_MEM_RAM_TERM);
			String disk = RequirementsHelper.getValueSmallerPerAttribute(requirements, RequirementsHelper.GLUE_DISK_TERM);
			return new Flavor("flavor", cpu, mem, disk);
		} 
		updateFlavors(token);
		return RequirementsHelper.findFlavor(getFlavors(),requirements);			
	}
	
	public List<String> getTemplatesInProperties(Properties properties) {
		List<String> listTemplate = new ArrayList<String>();
		String propertiesTample = (String) properties.get(OneConfigurationConstants.OPENNEBULA_TEMPLATES);
		if (propertiesTample != null) {
			String[] templates = propertiesTample.split(",");
			for (String template : templates) {
				listTemplate.add(template.trim());
			}
		}
		return listTemplate;
	}
}
