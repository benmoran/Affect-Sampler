/**
 * 
 */
package net.benmoran.affectsampler;

import java.sql.Timestamp;

public class Interval extends Object {
		public final Timestamp startTime;
		public final Timestamp endTime;
		public final int sequenceID;

		public Interval(Timestamp startTime, Timestamp endTime, int sequenceID) {
			this.startTime = startTime;
			this.endTime = endTime;
			this.sequenceID = sequenceID;
		}

		public Interval(Interval interval) {
			this.startTime = interval.startTime;
			this.endTime = interval.endTime;
			this.sequenceID = interval.sequenceID;
		}

		@Override
		public int hashCode() {
			int result = startTime.hashCode();
			result = 37 * result + endTime.hashCode();
			result = 37 * result + sequenceID;
			return result;
		}

		public boolean containsTimestamp(Timestamp t) {
			return t.after(startTime) && t.before(endTime);
		}

		@Override
		public String toString() {
			return "[" + sequenceID + "][" + startTime.toString() + "--" + endTime.toString() + "]";
		}

		@Override
		public boolean equals(Object o) {
			if (o == this)
				return true;
			if (o instanceof Interval) {
				Interval vo = (Interval) o;
				boolean equal;
				equal = (vo.startTime.equals(startTime));
				equal = equal && (vo.endTime.equals(endTime));
				equal = equal && (vo.sequenceID == sequenceID);
				return equal;
			}
			return false;
		}
	}