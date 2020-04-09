package tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import main.AbstractMain;

public class Stopwatch {

	private HashMap<String, Integer> typeMap;
	private ArrayList<ArrayList<Long>> times;

	public Stopwatch() {
		typeMap = new HashMap<>();
		times = new ArrayList<>();
	}

	public void add(String type, long time) {
		int id = typeToId(type);
		times.get(id).add(time);
	}

	public int getCount(String type) {
		if (!typeMap.containsKey(type)) {
			if (AbstractMain.DEBUG)
				System.err.println("Unknown type: " + type);
			return 0;
		}

		return times.get(typeMap.get(type)).size();
	}

	public double getRuntime(String type) {
		return getRuntime(type, "s");
	}

	public double getRuntime(String type, String unit) {
		if (!typeMap.containsKey(type)) {
			if (AbstractMain.DEBUG)
				System.err.println("Unknown type: " + type);
			return 0;
		}

		double factor = factor(unit);

		return sumList(times.get(typeMap.get(type))) / factor;
	}

	public double getTotalRuntime() {
		return getTotalRuntime("s");
	}

	public double getTotalRuntime(String unit) {
		double factor = factor(unit);
		return times.stream().mapToLong(this::sumList).reduce(0l, Long::sum) / factor;
	}

	private long sumList(ArrayList<Long> list) {
		return list.stream().reduce(0l, Long::sum);
	}

	@Override
	public String toString() {
		return toString("s");
	}

	public String toString(String unit) {
		double factor = factor(unit);

		StringBuilder sb = new StringBuilder();

		sb.append("Stopwatch[t=");

		double totalTime = getTotalRuntime(unit);

		sb.append(totalTime);
		sb.append(unit);

		double currTime;
		ArrayList<Long> currTimes;
		boolean first = true;
		sb.append(",[");
		for (Entry<String, Integer> type : typeMap.entrySet()) {
			currTimes = times.get(type.getValue());
			currTime = sumList(currTimes) / factor; // sum
			if (!first)
				sb.append(",");
			sb.append("(");
			sb.append(type.getKey());
			sb.append("=");
			sb.append(currTime);
			sb.append(unit);
			sb.append(",");
			sb.append(String.format("%.2f", currTime / totalTime * 100.0));
			sb.append("%,");
			sb.append(currTimes.size());
			sb.append(")");
			first = false;
		}
		sb.append("]]");

		return sb.toString();
	}

	public static double factor(String unit) {
		double factor = 1.0;
		switch (unit) {
		case "h":
			factor *= 60.0;
		case "min":
			factor *= 60.0;
		case "s":
			factor *= 1000.0;
		case "ms":
			factor *= 1.0;
			break;
		default:
			throw new IllegalArgumentException("Unknown unit " + unit + "!");
		}
		return factor;
	}

	private int typeToId(String type) {
		if (typeMap.containsKey(type)) {
			return typeMap.get(type);
		}

		// new type, update data structures
		int typeId = typeMap.size();
		typeMap.put(type, typeId);
		times.add(new ArrayList<>());
		return typeId;
	}

}
