package co.nubela.vault.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GasPriceResponse {
    private String jsonrpc;
    private int id;
    private String result;
}