package br.com.sauf_api.auth;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class SenhaValidaValidator implements ConstraintValidator<SenhaValida, String> {

    static final int MINIMO = 8;
    static final int MAXIMO = 72;
    private static final Pattern LETRA = Pattern.compile("\\p{L}");
    private static final Pattern ESPECIAL = Pattern.compile("[^\\p{L}\\p{N}\\s]");

    @Override
    public boolean isValid(String senha, ConstraintValidatorContext context) {
        String problema = problema(senha);
        if (problema == null) return true;

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(problema).addConstraintViolation();
        return false;
    }

    // A primeira regra que falhar, na mesma ordem da lista mostrada no front
    static String problema(String senha) {
        if (senha == null || senha.length() < MINIMO) return "A senha deve ter no mínimo 8 caracteres";
        if (senha.length() > MAXIMO) return "A senha deve ter no máximo 72 caracteres";
        if (!LETRA.matcher(senha).find()) return "A senha precisa de pelo menos 1 letra";
        if (!ESPECIAL.matcher(senha).find()) return "A senha precisa de pelo menos 1 caractere especial";
        return null;
    }
}
