"""File parsing utilities for fixed-width mainframe data files."""

from typing import Any


def parse_fixed_width_record(
    line: str,
    field_specs: list[tuple[str, int, int, type]]
) -> dict[str, Any]:
    """
    Parse a fixed-width record based on field specifications.
    
    This function mirrors the COBOL copybook field definitions where each field
    has a specific position and length in the record.
    
    Args:
        line: The raw line from the data file
        field_specs: List of tuples (field_name, start_pos, length, field_type)
                    where start_pos is 0-indexed
    
    Returns:
        Dictionary with field names as keys and parsed values
    
    Example:
        # COBOL: 05 ACCT-ID PIC 9(11) at position 0
        specs = [("acct_id", 0, 11, int)]
        result = parse_fixed_width_record(line, specs)
    """
    result: dict[str, Any] = {}
    
    for field_name, start_pos, length, field_type in field_specs:
        raw_value = line[start_pos:start_pos + length]
        
        if field_type == int:
            try:
                result[field_name] = int(raw_value.strip()) if raw_value.strip() else 0
            except ValueError:
                result[field_name] = 0
        elif field_type == float:
            try:
                result[field_name] = float(raw_value.strip()) if raw_value.strip() else 0.0
            except ValueError:
                result[field_name] = 0.0
        else:
            result[field_name] = raw_value.rstrip()
    
    return result


def parse_decimal_field(value: str, decimal_places: int = 2) -> float:
    """
    Parse a COBOL decimal field (PIC S9(n)V99 format).
    
    In COBOL, decimal values are often stored without the decimal point,
    with an implied decimal position (V in PIC clause).
    
    Args:
        value: The raw string value
        decimal_places: Number of implied decimal places
    
    Returns:
        The parsed decimal value as a float
    """
    try:
        clean_value = value.strip()
        if not clean_value:
            return 0.0
        
        is_negative = clean_value.startswith("-") or clean_value.endswith("-")
        clean_value = clean_value.replace("-", "").replace("+", "")
        
        if "." in clean_value:
            result = float(clean_value)
        else:
            int_value = int(clean_value)
            result = int_value / (10 ** decimal_places)
        
        return -result if is_negative else result
    except ValueError:
        return 0.0
