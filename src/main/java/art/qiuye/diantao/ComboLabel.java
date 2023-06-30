package art.qiuye.diantao;

import lombok.Data;

@Data
class ComboLabel<E>{
    private String label;
    private E value;
    private Integer arow;

    private String type;//work walk

    public ComboLabel(String label, E value) {
        this.label = label;
        this.value = value;
    }

    public ComboLabel(String label, E value, Integer arow, String type) {
        this.label = label;
        this.value = value;
        this.arow = arow;
        this.type = type;
    }

    public ComboLabel(String label, E value, Integer arow) {
        this.label = label;
        this.value = value;
        this.arow = arow;
    }

    @Override
    public String toString(){
        return label;
    }

}
