package be.bernardi.mvforandroid.beta.mvapi;

import org.json.JSONObject;

public class VikingPointsStats {
	private int	used_points;
	private int	unused_points;
	private int	waiting_points;
	private int	topups_used;
	private int	earned_points;

	public VikingPointsStats(JSONObject jsonObj) {
		this.used_points = jsonObj.optInt("used_points");
		this.unused_points = jsonObj.optInt("unused_points");
		this.waiting_points = jsonObj.optInt("waiting_points");
		this.topups_used = jsonObj.optInt("topups_used");
		this.earned_points = jsonObj.optInt("earned_points");
	}

	public int getUsed_points() {
		return used_points;
	}

	public int getUnused_points() {
		return unused_points;
	}

	public int getWaiting_points() {
		return waiting_points;
	}

	public int getTopups_used() {
		return topups_used;
	}

	public int getEarned_points() {
		return earned_points;
	}

	public String toString() {
		String out = "*****************[VIKING POINTS STATS]*****************\n";
		out += "** Earned points:\t" + earned_points + "\n";
		out += "** Used points:\t" + used_points + "\n";
		out += "** Unused points:\t" + unused_points + "\n";
		out += "** Waiting points:\t" + waiting_points + "\n";
		out += "** TopUps used:\t" + topups_used + "\n";
		out += "*******************************************************\n";
		return out;
	}
}
