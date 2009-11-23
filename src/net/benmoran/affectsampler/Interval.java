/*
 * 
 *  Copyright 2009 (C) Ben Moran
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); 
 *  you may not use this file except in compliance with the License. 
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0 
 *     
 *  Unless required by applicable law or agreed to in writing, 
 *  software distributed under the License is distributed on an 
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the specific 
 *  language governing permissions and limitations under the License. 
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