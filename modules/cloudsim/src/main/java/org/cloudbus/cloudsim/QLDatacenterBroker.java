package org.cloudbus.cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.VmList;

import java.util.ArrayList;
import java.util.List;

public class QLDatacenterBroker extends DatacenterBroker {

    public QLDatacenterBroker(String name) throws Exception {
        super(name);
    }
    public int randomInt(int min, int max){
        return (int)(Math.random()*(max-min+1)+min);
    }
    public Double computeQValue(float g, float a, Double q, Double max, int r){
        Double qValue;
        qValue = (q * (1 - a)) + (a * (r + (g * max)));
        return qValue;
    }

    public void scheduleTaskstoVms() {
        int reqTasks = cloudletList.size();
        int reqVms = vmList.size();
        int maxIndex = 0;
        int minIndex = 0;
        int maxQ = 0;
        float gamma = 0.1f;
        float alpha = 0.9f;
        Double epsilon = 1.0;
        Double decay = 0.1;
        Vm vm = vmList.get(0);
        Double[] qMatrix = new Double[reqVms];
        int[] rewardMatrix = new int[reqVms];
        Double[] cpuMatrix = new Double[reqVms];
        Log.printLine("number of loops " + Integer.toString(reqTasks));
        for(int i = 0; i < reqVms; i++){
            qMatrix[i] = 0.0;    
        }
        
        
        for (int i = 0; i < reqTasks; i++){
            
            int n = randomInt(0, 1);
            for (int j = 0; j < reqVms; j++){
                vm = vmList.get(j);
                cpuMatrix[j] = vm.getCpuUtilization();
                rewardMatrix[j] = 0;
                Double temp = cpuMatrix[j];
                Double qtemp = qMatrix[j];
                if (temp > cpuMatrix[maxIndex]){
                    maxIndex = j;
                }
                if (temp < cpuMatrix[minIndex]){
                    minIndex = j;
                }
                if (qtemp > qMatrix[j]){
                    maxQ = j;
                }
                
            }
            rewardMatrix[maxIndex] = -10;
            rewardMatrix[minIndex] = 100;
            if (n < epsilon){
                Log.printLine("===Exploitation===");
                int action = randomInt(0, reqVms);
                qMatrix[action] = computeQValue(gamma, alpha, qMatrix[action], qMatrix[maxQ], rewardMatrix[action]);
                bindCloudletToVm(i, action);
                System.out.println("Task" + cloudletList.get(i).getCloudletId() + " is bound with VM" + vmList.get(action).getId());   
                epsilon = epsilon - (epsilon*decay);             

            }
            else{
                Log.printLine("===Exploration===");
                qMatrix[maxQ] = computeQValue(gamma, alpha, qMatrix[maxQ], qMatrix[maxQ], rewardMatrix[maxQ]);
                bindCloudletToVm(i, maxQ);
                System.out.println("Task" + cloudletList.get(i).getCloudletId() + " is bound with VM" + vmList.get(maxQ).getId());   
                epsilon = epsilon - (epsilon*decay);
            }
        }
    
        
        /*
        for (int i = 0; i < reqTasks; i++) {
            bindCloudletToVm(i, (i % reqVms));
            System.out.println("Task" + cloudletList.get(i).getCloudletId() + " is bound with VM" + vmList.get(i % reqVms).getId());
        } */

        //System.out.println("reqTasks: "+ reqTasks);

        ArrayList<Cloudlet> list = new ArrayList<Cloudlet>();
        for (Cloudlet cloudlet : getCloudletReceivedList()) {
            list.add(cloudlet);
        }

        //setCloudletReceivedList(null);

        Cloudlet[] list2 = list.toArray(new Cloudlet[list.size()]);

        //System.out.println("size :"+list.size());

        Cloudlet temp = null;

        int n = list.size();

        for (int i = 0; i < n; i++) {
            for (int j = 1; j < (n - i); j++) {
                if (list2[j - 1].getCloudletLength() / (vm.getMips() * vm.getNumberOfPes()) > list2[j].getCloudletLength() / (vm.getMips() * vm.getNumberOfPes())) {
                    //swap the elements!
                    //swap(list2[j-1], list2[j]);
                    temp = list2[j - 1];
                    list2[j - 1] = list2[j];
                    list2[j] = temp;
                }
                // printNumbers(list2);
            }
        }

        ArrayList<Cloudlet> list3 = new ArrayList<Cloudlet>();

        for (int i = 0; i < list2.length; i++) {
            list3.add(list2[i]);
        }
        //printNumbers(list);

        setCloudletReceivedList(list);

        //System.out.println("\n\tSJFS Broker Schedules\n");
        //System.out.println("\n");
    }

    public void printNumber(Cloudlet[] list) {
        for (int i = 0; i < list.length; i++) {
            System.out.print(" " + list[i].getCloudletId());
            System.out.println(list[i].getCloudletStatusString());
        }
        System.out.println();
    }

    public void printNumbers(ArrayList<Cloudlet> list) {
        for (int i = 0; i < list.size(); i++) {
            System.out.print(" " + list.get(i).getCloudletId());
        }
        System.out.println();
    }

    /**
	 * Submit cloudlets to the created VMs.
	 * 
	 * @pre $none
	 * @post $none
         * @see #submitCloudletList(java.util.List) 
	 */
    @Override
	protected void submitCloudlets() {
		int vmIndex = 0;
		List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();
		for (Cloudlet cloudlet : getCloudletList()) {
			Vm vm;
			
			// if user didn't bind this cloudlet and it has not been executed yet
			if (cloudlet.getVmId() == -1) {
				vm = getVmsCreatedList().get(vmIndex);
				Log.printConcatLine("VM CPU util",vm.getTotalUtilizationOfCpu(System.currentTimeMillis()));

			} else { // submit to the specific vm
				vm = VmList.getById(getVmsCreatedList(), cloudlet.getVmId());
				if (vm == null) { // vm was not created
					if(!Log.isDisabled()) {				    
					    Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Postponing execution of cloudlet ",
							cloudlet.getCloudletId(), ": bount VM not available");
					}
					continue;
				}
			}

			if (!Log.isDisabled()) {
                Log.printConcatLine("vm utilization" + vm.getTotalUtilizationOfCpu(System.currentTimeMillis()));
			    Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Sending cloudlet ",
					cloudlet.getCloudletId(), " to VM #", vm.getId());
			}
			
			cloudlet.setVmId(vm.getId());
			sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
			cloudletsSubmitted++;
			vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
			getCloudletSubmittedList().add(cloudlet);
			successfullySubmitted.add(cloudlet);
		}

		// remove submitted cloudlets from waiting list
		getCloudletList().removeAll(successfullySubmitted);
	}


    @Override
    protected void processCloudletReturn(SimEvent ev) {
        Cloudlet cloudlet = (Cloudlet) ev.getData();
        getCloudletReceivedList().add(cloudlet);
        Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
                + " received");
        //Log.printLine("Scheduling..." + Integer.toString(getCloudletList().size()));
        //scheduleTaskstoVms();
        cloudletsSubmitted--;
        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
            Log.printLine("All cloudlets submitted");
            scheduleTaskstoVms();
            cloudletExecution(cloudlet);
        }
    }

    protected void cloudletExecution(Cloudlet cloudlet) {

        if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
            Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
            clearDatacenters();
            finishExecution();
        } else { // some cloudlets haven't finished yet
            if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
                // all the cloudlets sent finished. It means that some bount
                // cloudlet is waiting its VM be created
                clearDatacenters();
                createVmsInDatacenter(0);
            }
        }
    }

    @Override
    protected void processResourceCharacteristics(SimEvent ev) {
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);

        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            distributeRequestsForNewVmsAcrossDatacenters();
        }
    }

    protected void distributeRequestsForNewVmsAcrossDatacenters() {
        int numberOfVmsAllocated = 0;
        int i = 0;

        final List<Integer> availableDatacenters = getDatacenterIdsList();

        for (Vm vm : getVmList()) {
            int datacenterId = availableDatacenters.get(i++ % availableDatacenters.size());
            String datacenterName = CloudSim.getEntityName(datacenterId);

            if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
                Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId() + " in " + datacenterName);
                sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
                numberOfVmsAllocated++;
            }
        }

        setVmsRequested(numberOfVmsAllocated);
        setVmsAcks(0);
    }
}