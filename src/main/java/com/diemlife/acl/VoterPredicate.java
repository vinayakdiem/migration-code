package com.diemlife.acl;

import static org.apache.commons.lang3.BooleanUtils.isNotFalse;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

@FunctionalInterface
public interface VoterPredicate<T> {

    VotingResult test(final T candidate);

    enum VotingResult {
        For(true), Against(false), Abstain(null);

        private final Boolean result;

        VotingResult(final Boolean result) {
            this.result = result;
        }

        public Boolean getResult() {
            return result;
        }

        public VotingResult and(final VotingResult other) {
            if (other == null || Abstain.equals(other)) {
                return this;
            } else if (Abstain.equals(this)) {
                return isNotFalse(other.getResult())
                        ? Abstain
                        : Against;
            } else {
                return isTrue(this.getResult()) && isTrue(other.getResult())
                        ? For
                        : Against;
            }
        }
    }

}
