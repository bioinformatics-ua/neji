package pt.ua.tm.neji.statistics;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


/**
 *
 * @author david
 */
public class StatisticsEntry {
    private String name;
    private String group;
    private int occurrences;

    public StatisticsEntry(String name, String group, int occurrences) {
        this.name = name;
        this.occurrences = occurrences;
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    
    
    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StatisticsEntry other = (StatisticsEntry) obj;
        if (( this.name == null ) ? ( other.name != null ) : !this.name.equals(other.name)) {
            return false;
        }
        if (( this.group == null ) ? ( other.group != null ) : !this.group.equals(other.group)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 89 * hash + ( this.name != null ? this.name.hashCode() : 0 );
        hash = 89 * hash + ( this.group != null ? this.group.hashCode() : 0 );
        return hash;
    }
    
    
}
