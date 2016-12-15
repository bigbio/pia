package de.mpc.pia.modeller.score.comparator;

import java.util.Comparator;

import de.mpc.pia.modeller.score.ScoreModel;


public class ScoreComparator<T extends ScoreComparable> implements Comparator<T> {

    /** the index of the compared score model of the spectrum */
    private String scoreModelName;

    /** whether a higher score is better or not, if not set, the {@link ScoreComparable} must decide */
    private Boolean higherScoreBetter;


    public ScoreComparator() {
        super();
        this.scoreModelName = "";
        this.higherScoreBetter = null;
    }


    public ScoreComparator(String modelName) {
        super();
        this.scoreModelName = modelName;
        this.higherScoreBetter = null;
    }


    public ScoreComparator(String modelName, boolean higherScoreBetter) {
        this(modelName);
        this.higherScoreBetter = higherScoreBetter;
    }


    /**
     * Sets the index of the scores of a spectrum, which will be compared.
     */
    public void setComparedScoreModel(String modelName) {
        this.scoreModelName = modelName;
    }


    @Override
    public int compare(T o1, T o2) {

        ScoreModel score1, score2;
        int compRet;

        if (o1 != null) {
            score1 = o1.getCompareScore(scoreModelName);
        } else {
            score1 = null;
        }

        if (o2 != null) {
            score2 = o2.getCompareScore(scoreModelName);
        } else {
            score2 = null;
        }

        // both PSMs don't have the score with given index
        if ((score1 == null) && (score2 == null)) compRet = 0;
        else if (score1 == null) {
            // score1 does not have the score with given index, but score2 does
            compRet = 1;
        } else if (score2 == null) {
            // score2 does not have the score with given index, but score1 does
            compRet = -1;
        } else {
            // both have the score model with given index
            if (higherScoreBetter != null) {
                compRet = score1.compareTo(score2, higherScoreBetter);
            } else {
                compRet = score1.compareTo(score2);
            }
        }

        return compRet;
    }

    @Override
    public String toString() {
        return scoreModelName + ":" + higherScoreBetter;
    }
}
