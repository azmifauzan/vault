package co.nubela.vault.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InfuraRequest {
    private int id;
    private String jsonrpc = "2.0";
    private String method;
    private List<Object> params;
}
