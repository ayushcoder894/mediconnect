import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { Badge, Button, Stars } from "../components/ui";

describe("ui components", () => {
  it("shows a readable label for a status badge", () => {
    render(<Badge status="NO_SHOW" />);
    expect(screen.getByText("No Show")).toBeInTheDocument();
  });

  it("disables a button while loading", () => {
    render(<Button loading>Save</Button>);
    expect(screen.getByRole("button", { name: "Save" })).toBeDisabled();
  });

  it("reports the clicked star rating", () => {
    const onChange = vi.fn();
    render(<Stars value={3} onChange={onChange} />);
    fireEvent.click(screen.getByLabelText("5 stars"));
    expect(onChange).toHaveBeenCalledWith(5);
  });
});
