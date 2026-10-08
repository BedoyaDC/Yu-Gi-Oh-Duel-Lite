package ygo.model;

import java.awt.Image;

/** Modelo de una carta Monster: nombre, ATK, DEF e imagen oficial. */
public class Card {
    private final int id;
    private final String name;
    private final String type;   // ej. "Effect Monster", "Link Monster"
    private final int atk;
    private final int def;       // 0 si la carta no tiene DEF (Link Monsters)
    private final String imageUrl;
    private final Image image;   // puede ser null si la descarga falló

    public Card(int id, String name, String type, int atk, int def, String imageUrl, Image image) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.atk = atk;
        this.def = def;
        this.imageUrl = imageUrl;
        this.image = image;
    }

    /** Los Link Monsters no tienen DEF ni pueden jugarse en defensa. */
    public boolean isLink() { return type.contains("Link"); }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getAtk() { return atk; }
    public int getDef() { return def; }
    public String getImageUrl() { return imageUrl; }
    public Image getImage() { return image; }
}
