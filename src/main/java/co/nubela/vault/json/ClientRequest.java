package co.nubela.vault.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClientRequest {
    private String id;
    private String type;
    private String from_address;
    private String to_address;
    private String amount;
}
