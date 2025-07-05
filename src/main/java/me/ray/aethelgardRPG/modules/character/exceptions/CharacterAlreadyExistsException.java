package me.ray.aethelgardRPG.modules.character.exceptions;

public class CharacterAlreadyExistsException extends RuntimeException {
    public CharacterAlreadyExistsException(String message) {
        super(message);
    }
}