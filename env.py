#!/usr/bin/env python3
"""
Envelope PDF Generator (Pure Python - No External Dependencies)

1. Prompts for return address selection first (default is 1st entry in list).
2. Prompts for envelope type (#10, #10 windowed, #9, #6).
3. If windowed envelope is chosen, recipient address entry is skipped.
4. If standard envelope is chosen, queries line-by-line for recipient address.
5. Generates PDF file directly using Python standard library.
"""

import os
import sys

# ==============================================================================
# RETURN ADDRESSES (Update or add entries before running the program)
# ==============================================================================
RETURN_ADDRESSES = [
    {
        "name": "David Morrison",
        "street": "1900 Grace Ave",
        "street2": "#242",
        "city": "Harlingen",
        "state": "TX",
        "zip": "78550",
    },
    {
        "name": "Barreras",
        "street": "1900 Grace Ave",
        "street2": "#223",
        "city": "Harlingen",
        "state": "TX",
        "zip": "78550",
    },
]

# Default return address for backward compatibility
RETURN_ADDRESS = RETURN_ADDRESSES[0]

# Points per inch (72 points = 1 inch in PDF standard space)
INCH = 72.0

# ==============================================================================
# ENVELOPE CONFIGURATIONS (#10, #10 windowed, #9, #6)
# ==============================================================================
ENVELOPES = {
    "1": {
        "name": "#10",
        "description": "Standard Commercial Envelope (9.5\" x 4.125\")",
        "width_pt": 9.5 * INCH,
        "height_pt": 4.125 * INCH,
        "recipient_left": 4.25 * INCH,
        "recipient_bottom": 1.9 * INCH,
        "filename": "envelope_no10.pdf",
        "is_windowed": False
    },
    "2": {
        "name": "#10 windowed",
        "description": "#10 Windowed Envelope (9.5\" x 4.125\")",
        "width_pt": 9.5 * INCH,
        "height_pt": 4.125 * INCH,
        "recipient_left": 4.25 * INCH,
        "recipient_bottom": 1.9 * INCH,
        "filename": "envelope_no10_windowed.pdf",
        "is_windowed": True
    },
    "3": {
        "name": "#9",
        "description": "#9 Commercial Envelope (8.875\" x 3.875\")",
        "width_pt": 8.875 * INCH,
        "height_pt": 3.875 * INCH,
        "recipient_left": 3.875 * INCH,
        "recipient_bottom": 1.75 * INCH,
        "filename": "envelope_no9.pdf",
        "is_windowed": False
    },
    "4": {
        "name": "#6",
        "description": "#6 Personal / Commercial Envelope (6.5\" x 3.625\")",
        "width_pt": 6.5 * INCH,
        "height_pt": 3.625 * INCH,
        "recipient_left": 2.75 * INCH,
        "recipient_bottom": 1.5 * INCH,
        "filename": "envelope_no6.pdf",
        "is_windowed": False
    }
}

def pdf_escape(text):
    """Escapes special characters in PDF literal string."""
    if text is None:
        return ""
    return str(text).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")

def write_pure_pdf(filename, width_pt, height_pt, stream_commands):
    """Writes a standard PDF file without third-party dependencies."""
    stream_content = "\n".join(stream_commands)
    stream_bytes = stream_content.encode("utf-8")
    stream_len = len(stream_bytes)

    objects = [
        # Obj 1: Catalog
        "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj",
        # Obj 2: Pages tree
        "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj",
        # Obj 3: Page object
        f"3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {width_pt:.2f} {height_pt:.2f}] /Resources << /Font << /F1 4 0 R /F2 5 0 R /F3 6 0 R >> >> /Contents 7 0 R >>\nendobj",
        # Obj 4: Helvetica-Bold
        "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>\nendobj",
        # Obj 5: Helvetica
        "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj",
        # Obj 6: Helvetica-Oblique
        "6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique >>\nendobj",
        # Obj 7: Contents Stream
        f"7 0 obj\n<< /Length {stream_len} >>\nstream\n{stream_content}\nendstream\nendobj"
    ]

    header = "%PDF-1.4\n%\xe2\xe3\xcf\xd3\n"
    body_offsets = []
    current_offset = len(header.encode("latin1"))

    pdf_body = ""
    for obj in objects:
        body_offsets.append(current_offset)
        pdf_body += obj + "\n"
        current_offset += len((obj + "\n").encode("utf-8"))

    xref_offset = current_offset
    xref = f"xref\n0 {len(objects) + 1}\n0000000000 65535 f \n"
    for offset in body_offsets:
        xref += f"{offset:010d} 00000 n \n"

    trailer = f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF\n"

    with open(filename, "wb") as f:
        f.write(header.encode("latin1"))
        f.write(pdf_body.encode("utf-8"))
        f.write(xref.encode("utf-8"))
        f.write(trailer.encode("utf-8"))

def select_return_address():
    """Displays list of return addresses and gets user selection."""
    print("\n" + "=" * 50)
    print("             SELECT RETURN ADDRESS")
    print("=" * 50)
    for idx, addr in enumerate(RETURN_ADDRESSES, start=1):
        parts = [addr.get("name", ""), addr.get("street", ""), addr.get("street2", ""), f"{addr.get('city', '')}, {addr.get('state', '')} {addr.get('zip', '')}".strip()]
        formatted = ", ".join([p for p in parts if p])
        default_str = " (Default)" if idx == 1 else ""
        print(f"  [{idx}] {formatted}{default_str}")

    while True:
        choice = input(f"\nSelect return address (1-{len(RETURN_ADDRESSES)}) [default: 1]: ").strip()
        if choice == "":
            return RETURN_ADDRESSES[0]
        if choice.isdigit():
            val = int(choice)
            if 1 <= val <= len(RETURN_ADDRESSES):
                return RETURN_ADDRESSES[val - 1]
        print(f"Invalid choice. Please enter a number between 1 and {len(RETURN_ADDRESSES)}.")

def select_envelope():
    """Displays list of envelopes and gets user selection."""
    print("\n" + "=" * 50)
    print("             SELECT ENVELOPE SIZE")
    print("=" * 50)
    for key, env in ENVELOPES.items():
        print(f"  [{key}] {env['name']} - {env['description']}")

    while True:
        choice = input("\nSelect envelope (1-4): ").strip()
        if choice in ENVELOPES:
            return ENVELOPES[choice]
        print("Invalid choice. Please enter 1, 2, 3, or 4.")

def query_recipient_address():
    """Queries user line-by-line for recipient envelope address."""
    print("\n" + "=" * 50)
    print("        ENVELOPE RECIPIENT ADDRESS ENTRY")
    print("=" * 50)
    name = input("Name: ").strip()
    street = input("Street address: ").strip()
    street2 = input("Street address line 2 (optional): ").strip()
    city = input("City: ").strip()
    state = input("State: ").strip()
    zip_code = input("Zip: ").strip()

    return {
        "name": name,
        "street": street,
        "street2": street2,
        "city": city,
        "state": state,
        "zip": zip_code
    }

def generate_envelope_pdf(envelope_config, recipient=None, return_addr=None, output_filename=None):
    """Generates envelope PDF file directly without external libraries."""
    if return_addr is None:
        return_addr = RETURN_ADDRESSES[0]

    if output_filename is None:
        output_filename = envelope_config["filename"]

    width_pt = envelope_config["width_pt"]
    height_pt = envelope_config["height_pt"]

    commands = []

    # ---------------------------------------------------------
    # 1. Return Address (Top-Left corner)
    # ---------------------------------------------------------
    return_left = 0.5 * INCH
    return_top = height_pt - 0.5 * INCH
    line_spacing = 13.0

    ret_name = pdf_escape(return_addr.get("name", ""))
    ret_street = pdf_escape(return_addr.get("street", ""))
    ret_street2 = pdf_escape(return_addr.get("street2", ""))
    ret_city_state_zip = pdf_escape(f"{return_addr.get('city', '')}, {return_addr.get('state', '')} {return_addr.get('zip', '')}".strip())

    ret_lines = [
        (ret_name, "/F1 9.5 Tf"),
        (ret_street, "/F2 9.5 Tf"),
    ]
    if ret_street2:
        ret_lines.append((ret_street2, "/F2 9.5 Tf"))
    ret_lines.append((ret_city_state_zip, "/F2 9.5 Tf"))

    for idx, (line_text, font_cmd) in enumerate(ret_lines):
        if not line_text:
            continue
        y_pos = return_top - (idx * line_spacing)
        commands.extend([
            "BT",
            f"{font_cmd}",
            f"1 0 0 1 {return_left:.2f} {y_pos:.2f} Tm",
            f"({line_text}) Tj",
            "ET"
        ])

    # ---------------------------------------------------------
    # 2. Delivery / Recipient Address
    # ---------------------------------------------------------
    if envelope_config["is_windowed"]:
        # Windowed envelope: recipient address appears via window insert, print only return address
        pass
    elif recipient is not None:
        recipient_left = envelope_config["recipient_left"]
        recipient_baseline = envelope_config["recipient_bottom"]
        recip_line_spacing = 16.0

        recip_name = pdf_escape(recipient.get("name", ""))
        recip_street = pdf_escape(recipient.get("street", ""))
        recip_street2 = pdf_escape(recipient.get("street2", ""))
        recip_city_state_zip = pdf_escape(f"{recipient.get('city', '')}, {recipient.get('state', '')} {recipient.get('zip', '')}".strip())

        recip_lines = [
            (recip_name, "/F1 11.5 Tf"),
            (recip_street, "/F2 11.5 Tf"),
        ]
        if recip_street2:
            recip_lines.append((recip_street2, "/F2 11.5 Tf"))
        recip_lines.append((recip_city_state_zip, "/F2 11.5 Tf"))

        num_lines = len(recip_lines)
        for idx, (line_text, font_cmd) in enumerate(recip_lines):
            if not line_text:
                continue
            y_pos = recipient_baseline + ((num_lines - 1 - idx) * recip_line_spacing)
            commands.extend([
                "BT",
                f"{font_cmd}",
                f"1 0 0 1 {recipient_left:.2f} {y_pos:.2f} Tm",
                f"({line_text}) Tj",
                "ET"
            ])

    write_pure_pdf(output_filename, width_pt, height_pt, commands)
    print(f"\nEnvelope PDF generated successfully: {os.path.abspath(output_filename)}")
    return output_filename

def main():
    # 1. Select return address FIRST
    return_addr = select_return_address()

    # 2. Select envelope size
    envelope = select_envelope()

    # 3. If envelope is windowed, skip asking for recipient address info
    if envelope["is_windowed"]:
        print("\nWindowed envelope selected. Skipping recipient address prompt.")
        recipient = None
    else:
        # Query recipient address line-by-line for non-windowed envelopes
        recipient = query_recipient_address()

    # 4. Generate PDF for appropriate size with selected return address
    generate_envelope_pdf(envelope, recipient, return_addr=return_addr)

if __name__ == "__main__":
    main()

