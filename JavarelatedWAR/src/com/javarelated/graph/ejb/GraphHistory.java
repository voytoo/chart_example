package com.javarelated.graph.ejb;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Random;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

/**
 * Singleton Bean implementation class GraphHistory
 */
@Singleton
@LocalBean
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class GraphHistory {

	private static final int SECONDS_TO_PRESERVE = 1000;

	private int active;
	private int idle;
	private int waiting;
	private Random random;

	private ConcurrentSkipListMap<Long, int[]> historySkipListMap;

	@PostConstruct
	private void init() {
		active = 10;
		idle = 20;
		waiting = 5;
		random = new Random();
		historySkipListMap = new ConcurrentSkipListMap<Long, int[]>();
	}

    @Schedule(second="*/1", minute="*",hour="*", persistent=false)
    public void pushData(){
        addToMap(historySkipListMap, getActive(), getIdle(), getWaiting());
        if (isFull(historySkipListMap)) {
            historySkipListMap = getTail(historySkipListMap);
        }
    }
    
    public String getData(int interval) {
        return getJsonData(historySkipListMap, interval);
    }

    private int getActive() {
    	return Math.abs(active += random.nextInt() % 20);
    }

    private int getIdle() {
    	return Math.abs(idle += random.nextInt() % 20);
    }

    private int getWaiting() {
    	return Math.abs(waiting += random.nextInt() % 20);
    }

    private ConcurrentSkipListMap<Long, int[]> getTail(ConcurrentSkipListMap<Long, int[]> skipListMap) {
        double currentTime = System.currentTimeMillis();
        long second = Math.round(currentTime / 1000L);
        ConcurrentNavigableMap<Long, int[]> tailMap = skipListMap.tailMap(second - SECONDS_TO_PRESERVE);
        NavigableSet<Long> entrySet = tailMap.navigableKeySet();
        Iterator<Long> it = entrySet.iterator();
        ConcurrentSkipListMap<Long, int[]> newMap = new ConcurrentSkipListMap<Long, int[]>();
        while (it.hasNext()) {
            Long key = it.next();
            newMap.put(key, tailMap.get(key));
        }
        return newMap;
    }

    private boolean isFull(ConcurrentSkipListMap<Long, int[]> skipListMap) {
        return skipListMap.size()>SECONDS_TO_PRESERVE*2;
    }    

    private void addToMap(ConcurrentSkipListMap<Long, int[]> skipListMap, int active, int idle, int waiting){
        double currentTime = System.currentTimeMillis();
        long second = Math.round(currentTime / 1000L);

        int[] data = {active, idle, waiting};

        skipListMap.putIfAbsent(second, data);
    }

    private String getJsonData(ConcurrentNavigableMap<Long, int[]> skipListMap, int interval) {
        double currentTime = System.currentTimeMillis();
        long base = Math.round(currentTime / 1000L) - interval;
        return getHistoryData(skipListMap.tailMap(base), base, interval);
    }

    private String getHistoryData(ConcurrentNavigableMap<Long, int[]> tailMap, long base, int interval) {

        NavigableSet<Long> entrySet = tailMap.navigableKeySet();
        Iterator<Long> it = entrySet.iterator();

        JsonArrayBuilder activeStreamArray = Json.createArrayBuilder();
        JsonArrayBuilder waitingStreamArray = Json.createArrayBuilder();
        JsonArrayBuilder idleStreamArray = Json.createArrayBuilder();

        int counter = 0;
        while (it.hasNext() && counter < interval) {
            Long key = it.next();

            activeStreamArray.add(Json.createObjectBuilder().add("x", key-base).add("y", tailMap.get(key)[0]));
            idleStreamArray.add(Json.createObjectBuilder().add("x", key-base).add("y", tailMap.get(key)[1]));
            waitingStreamArray.add(Json.createObjectBuilder().add("x", key - base).add("y", tailMap.get(key)[2]));
            counter++;
        }

        JsonArray model = Json.createArrayBuilder().
        		add(getStream("active", activeStreamArray)).
        		add(getStream("idle", idleStreamArray)).
        		add(getStream("waiting", waitingStreamArray)).
        		build();

        StringWriter stWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(stWriter);
        jsonWriter.writeArray(model);
        jsonWriter.close();
        return stWriter.toString();
    }

    private JsonObjectBuilder getStream(String name, JsonArrayBuilder arrayBuilder) {
        return Json.createObjectBuilder().add("key", name).add("values", arrayBuilder);
    }

}
