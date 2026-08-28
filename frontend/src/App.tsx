import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import { AuthProvider } from "./auth/AuthContext";
import { BookPage } from "./pages/BookPage";
import { CatalogPage } from "./pages/CatalogPage";
import { ClubLayout } from "./pages/ClubLayout";
import { GatePage } from "./pages/GatePage";
import { WaitlistPage } from "./pages/WaitlistPage";

export function App() {
	return (
		<AuthProvider>
			<BrowserRouter>
				<Routes>
					<Route path="/" element={<GatePage />} />
					<Route element={<ClubLayout />}>
						<Route path="/catalog" element={<CatalogPage />} />
						<Route path="/book" element={<BookPage />} />
						<Route path="/waitlist" element={<WaitlistPage />} />
					</Route>
					<Route path="*" element={<Navigate to="/" replace />} />
				</Routes>
			</BrowserRouter>
		</AuthProvider>
	);
}
